-- ============================================================
-- Jachwi-in DB Schema
-- MySQL 8.0+
-- ============================================================

-- docker-entrypoint-initdb.d가 이 스크립트를 실행하는 mysql 클라이언트의 기본 커넥션 문자셋이
-- utf8mb4가 아니어서(latin1 등) 한글 컬럼명이 깨져 syntax error가 나는 걸 방지하기 위한 설정
SET NAMES utf8mb4;

-- 1. users (Auth Server가 owns, Main Server는 user_id만 참조)
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    email       VARCHAR(255)    NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    name        VARCHAR(100)    NOT NULL,
    nickname    VARCHAR(100)    NOT NULL,
    school      VARCHAR(100),
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 2. building (공공데이터 기반, 한글 컬럼명 유지)
--    기존 복합키(x, y) → surrogate PK(id) + UNIQUE INDEX(x, y)
CREATE TABLE IF NOT EXISTS building (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    x               DOUBLE          NOT NULL COMMENT '경도',
    y               DOUBLE          NOT NULL COMMENT '위도',
    시도명           VARCHAR(100),
    시군구           VARCHAR(100),
    법정읍면동명      VARCHAR(100),
    도로명           VARCHAR(255),
    건물본번         INT,
    건물부번         INT,
    건축물대장_건물명  VARCHAR(255),
    상세건물명        VARCHAR(255),
    시군구용_건물명   VARCHAR(255),
    버스정류장        INT DEFAULT 0,
    편의점           INT DEFAULT 0,
    카페             INT DEFAULT 0,
    가로등           INT DEFAULT 0,
    CCTV            INT DEFAULT 0,
    병원             INT DEFAULT 0,
    식당             INT DEFAULT 0,
    학교_거리        DOUBLE DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_building_xy (x, y),
    INDEX idx_building_district (시군구)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 3. posts (커뮤니티 게시글)
CREATE TABLE IF NOT EXISTS posts (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id     BIGINT          NOT NULL COMMENT 'users.id 참조',
    title       VARCHAR(100)    NOT NULL,
    content     TEXT            NOT NULL,
    category    VARCHAR(20)     NOT NULL COMMENT 'QUESTION|REVIEW|TIP|INFO|ROOMMATE|ETC',
    view_count  INT             NOT NULL DEFAULT 0,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_posts_user_id   (user_id),
    INDEX idx_posts_category  (category),
    INDEX idx_posts_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 4. comments (댓글 + 대댓글)
CREATE TABLE IF NOT EXISTS comments (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    post_id     BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL COMMENT 'users.id 참조',
    parent_id   BIGINT      NULL COMMENT 'NULL이면 최상위 댓글',
    content     TEXT        NOT NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_comments_post    FOREIGN KEY (post_id)   REFERENCES posts    (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent  FOREIGN KEY (parent_id) REFERENCES comments (id) ON DELETE CASCADE,
    INDEX idx_comments_post_id   (post_id),
    INDEX idx_comments_user_id   (user_id),
    INDEX idx_comments_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 5. bookmarks (관심 건물)
CREATE TABLE IF NOT EXISTS bookmarks (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL COMMENT 'users.id 참조',
    building_id BIGINT      NOT NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bookmark_user_building (user_id, building_id),
    CONSTRAINT fk_bookmarks_building FOREIGN KEY (building_id) REFERENCES building (id) ON DELETE CASCADE,
    INDEX idx_bookmarks_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
