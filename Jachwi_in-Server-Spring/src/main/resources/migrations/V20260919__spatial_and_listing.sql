-- Manual, ONE-TIME upgrade from the original schema.sql. Back up first.
-- Stop application/ETL writes. Check invalid coordinates before ALTER (see docs/spatial.md).
-- DDL commits implicitly. On failure inspect SHOW CREATE TABLE; do not blindly rerun.
SET NAMES utf8mb4;
ALTER TABLE building
    ADD COLUMN building_type VARCHAR(30) NOT NULL DEFAULT 'ETC' COMMENT 'APT|OFFICETEL|MULTI_FAMILY|DETACHED_HOUSE|ETC',
    ADD COLUMN sale_price BIGINT COMMENT '만원, 건물 요약값; 실제 매물 가격과 구분',
    ADD COLUMN jeonse_price BIGINT COMMENT '만원',
    ADD COLUMN monthly_rent_deposit BIGINT COMMENT '만원, monthly_rent와 동일한 표본',
    ADD COLUMN monthly_rent BIGINT COMMENT '만원',
    ADD COLUMN representative_price_type VARCHAR(20) COMMENT 'SALE|JEONSE|MONTHLY_RENT; NULL=미정',
    ADD COLUMN representative_price BIGINT COMMENT '만원; MONTHLY_RENT이면 월세 (보증금은 별도)',
    ADD COLUMN amenity_updated_at DATETIME,
    ADD COLUMN location POINT GENERATED ALWAYS AS (ST_GeomFromText(CONCAT('POINT(', x, ' ', y, ')'), 4326, 'axis-order=long-lat')) STORED NOT NULL SRID 4326,
    ADD INDEX idx_building_type (building_type),
    ADD SPATIAL INDEX idx_building_location (location),
    ADD CONSTRAINT chk_building_coordinates CHECK (x > -180 AND x <= 180 AND y >= -90 AND y <= 90),
    ADD CONSTRAINT chk_building_type CHECK (building_type IN ('APT','OFFICETEL','MULTI_FAMILY','DETACHED_HOUSE','ETC')),
    ADD CONSTRAINT chk_building_prices CHECK (
        (sale_price IS NULL OR sale_price >= 0) AND (jeonse_price IS NULL OR jeonse_price >= 0)
        AND (monthly_rent_deposit IS NULL OR monthly_rent_deposit >= 0)
        AND (monthly_rent IS NULL OR monthly_rent >= 0)),
    ADD CONSTRAINT chk_building_representative_price CHECK (
        (representative_price_type IS NULL AND representative_price IS NULL) OR
        (representative_price_type IS NOT NULL AND representative_price IS NOT NULL AND
         representative_price >= 0 AND representative_price_type IN ('SALE','JEONSE','MONTHLY_RENT')));

-- 6. 실제 매물: 기존 건물 추천 API와 별개, 가격 단위는 만원.
CREATE TABLE IF NOT EXISTS room_listing (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    user_id BIGINT,
    room_type VARCHAR(30) NOT NULL,
    trade_type VARCHAR(20) NOT NULL DEFAULT 'MONTHLY_RENT',
    deposit BIGINT,
    monthly_rent BIGINT,
    sale_price BIGINT,
    maintenance_fee BIGINT DEFAULT 0,
    floor INT,
    area DOUBLE,
    photos TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_listing_building FOREIGN KEY (building_id) REFERENCES building(id) ON DELETE CASCADE,
    CONSTRAINT fk_listing_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_listing_trade CHECK (trade_type IN ('SALE','JEONSE','MONTHLY_RENT')),
    CONSTRAINT chk_listing_status CHECK (status IN ('AVAILABLE','RESERVED','CLOSED')),
    CONSTRAINT chk_listing_price CHECK (
        (trade_type = 'SALE' AND sale_price IS NOT NULL AND sale_price >= 0 AND deposit IS NULL AND monthly_rent IS NULL) OR
        (trade_type = 'JEONSE' AND deposit IS NOT NULL AND deposit >= 0 AND monthly_rent IS NULL AND sale_price IS NULL) OR
        (trade_type = 'MONTHLY_RENT' AND deposit IS NOT NULL AND deposit >= 0 AND monthly_rent IS NOT NULL AND monthly_rent >= 0 AND sale_price IS NULL)),
    INDEX idx_listing_building_trade (building_id, trade_type),
    INDEX idx_listing_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 통합 실거래 이력; 기존 apt_trade 원천 테이블/로더는 유지.
CREATE TABLE IF NOT EXISTS building_trade (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT,
    trade_type VARCHAR(20) NOT NULL,
    area DOUBLE,
    floor VARCHAR(30),
    deal_date DATE,
    price BIGINT COMMENT '만원: SALE=매매가, JEONSE/MONTHLY_RENT=보증금',
    monthly_rent BIGINT COMMENT '만원',
    source VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trade_building FOREIGN KEY (building_id) REFERENCES building(id),
    CONSTRAINT chk_trade_type CHECK (trade_type IN ('SALE','JEONSE','MONTHLY_RENT')),
    INDEX idx_trade_building_date (building_id, deal_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cafe (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    x DOUBLE NOT NULL COMMENT '경도',
    y DOUBLE NOT NULL COMMENT '위도',
    location POINT GENERATED ALWAYS AS (ST_GeomFromText(CONCAT('POINT(', x, ' ', y, ')'), 4326, 'axis-order=long-lat')) STORED NOT NULL SRID 4326,
    source VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_cafe_coordinates CHECK (x > -180 AND x <= 180 AND y >= -90 AND y <= 90),
    SPATIAL INDEX idx_cafe_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS convenience_store (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    x DOUBLE NOT NULL COMMENT '경도',
    y DOUBLE NOT NULL COMMENT '위도',
    location POINT GENERATED ALWAYS AS (ST_GeomFromText(CONCAT('POINT(', x, ' ', y, ')'), 4326, 'axis-order=long-lat')) STORED NOT NULL SRID 4326,
    source VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_convenience_store_coordinates CHECK (x > -180 AND x <= 180 AND y >= -90 AND y <= 90),
    SPATIAL INDEX idx_convenience_store_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hospital (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    x DOUBLE NOT NULL COMMENT '경도',
    y DOUBLE NOT NULL COMMENT '위도',
    location POINT GENERATED ALWAYS AS (ST_GeomFromText(CONCAT('POINT(', x, ' ', y, ')'), 4326, 'axis-order=long-lat')) STORED NOT NULL SRID 4326,
    source VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_hospital_coordinates CHECK (x > -180 AND x <= 180 AND y >= -90 AND y <= 90),
    SPATIAL INDEX idx_hospital_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS restaurant (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    x DOUBLE NOT NULL COMMENT '경도',
    y DOUBLE NOT NULL COMMENT '위도',
    location POINT GENERATED ALWAYS AS (ST_GeomFromText(CONCAT('POINT(', x, ' ', y, ')'), 4326, 'axis-order=long-lat')) STORED NOT NULL SRID 4326,
    source VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_restaurant_coordinates CHECK (x > -180 AND x <= 180 AND y >= -90 AND y <= 90),
    SPATIAL INDEX idx_restaurant_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cctv (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    x DOUBLE NOT NULL COMMENT '경도',
    y DOUBLE NOT NULL COMMENT '위도',
    location POINT GENERATED ALWAYS AS (ST_GeomFromText(CONCAT('POINT(', x, ' ', y, ')'), 4326, 'axis-order=long-lat')) STORED NOT NULL SRID 4326,
    source VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_cctv_coordinates CHECK (x > -180 AND x <= 180 AND y >= -90 AND y <= 90),
    SPATIAL INDEX idx_cctv_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS streetlight (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    x DOUBLE NOT NULL COMMENT '경도',
    y DOUBLE NOT NULL COMMENT '위도',
    location POINT GENERATED ALWAYS AS (ST_GeomFromText(CONCAT('POINT(', x, ' ', y, ')'), 4326, 'axis-order=long-lat')) STORED NOT NULL SRID 4326,
    source VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_streetlight_coordinates CHECK (x > -180 AND x <= 180 AND y >= -90 AND y <= 90),
    SPATIAL INDEX idx_streetlight_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS school (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    school_level VARCHAR(30),
    x DOUBLE NOT NULL COMMENT '경도',
    y DOUBLE NOT NULL COMMENT '위도',
    location POINT GENERATED ALWAYS AS (ST_GeomFromText(CONCAT('POINT(', x, ' ', y, ')'), 4326, 'axis-order=long-lat')) STORED NOT NULL SRID 4326,
    source VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_school_coordinates CHECK (x > -180 AND x <= 180 AND y >= -90 AND y <= 90),
    SPATIAL INDEX idx_school_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS subway_station (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    line VARCHAR(100),
    x DOUBLE NOT NULL COMMENT '경도',
    y DOUBLE NOT NULL COMMENT '위도',
    location POINT GENERATED ALWAYS AS (ST_GeomFromText(CONCAT('POINT(', x, ' ', y, ')'), 4326, 'axis-order=long-lat')) STORED NOT NULL SRID 4326,
    source VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_subway_station_coordinates CHECK (x > -180 AND x <= 180 AND y >= -90 AND y <= 90),
    SPATIAL INDEX idx_subway_station_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bus_stop (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    stop_id VARCHAR(100),
    routes VARCHAR(1000),
    x DOUBLE NOT NULL COMMENT '경도',
    y DOUBLE NOT NULL COMMENT '위도',
    location POINT GENERATED ALWAYS AS (ST_GeomFromText(CONCAT('POINT(', x, ' ', y, ')'), 4326, 'axis-order=long-lat')) STORED NOT NULL SRID 4326,
    source VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_bus_stop_coordinates CHECK (x > -180 AND x <= 180 AND y >= -90 AND y <= 90),
    SPATIAL INDEX idx_bus_stop_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
