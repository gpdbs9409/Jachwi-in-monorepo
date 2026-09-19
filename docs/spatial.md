# MySQL 공간 설계와 마이그레이션

## 좌표와 인덱스

MySQL 8.0.16 이상(CHECK 제약 지원)이 필요합니다. 검증 기준은 MySQL 8.0.46입니다.
`schema.sql`이 새 설치 DDL의 기준이며 `building`과 다음 POI 테이블에 같은 패턴을 적용합니다:
`cafe`, `convenience_store`, `hospital`, `restaurant`, `cctv`, `streetlight`,
`school`, `subway_station`, `bus_stop`.

```sql
x DOUBLE NOT NULL, -- 경도
y DOUBLE NOT NULL, -- 위도
location POINT GENERATED ALWAYS AS (
    ST_GeomFromText(CONCAT('POINT(', x, ' ', y, ')'), 4326, 'axis-order=long-lat')
) STORED NOT NULL SRID 4326,
SPATIAL INDEX idx_building_location (location)
```

EPSG:4326의 기본 WKT 축 순서는 위도/경도이므로 생성과 검색 모두
`axis-order=long-lat`를 명시합니다. x/y는 유지하고 별도 longitude/latitude 중복 컬럼은 만들지 않습니다.
생성 컬럼은 INSERT/UPDATE 시 자동으로 동기화되므로 Java/CSV/Python에서 이중 쓰기를 할 필요가 없습니다.
명시적 컬럼 목록으로 x/y를 입력하고 `location`에는 값을 지정하지 마세요.
좌표는 경도 `(-180,180]`, 위도 `[-90,90]`이며, 좌표계가 다른 원천 데이터는 먼저 WGS84로 변환합니다.

관련 공식 설명: [공간 타입과 SRID](https://dev.mysql.com/doc/refman/8.0/en/spatial-type-overview.html),
[WKT 축 순서](https://dev.mysql.com/worklog/task/?id=9434),
[공간 인덱스 최적화](https://dev.mysql.com/doc/refman/8.0/en/spatial-index-optimization.html).

지도 조회는 `MULTIPOINT`의 MBR와 `MBRIntersects`를 사용하여 경계 좌표도 포함합니다.
원래 `BETWEEN` 조건도 유지합니다. 극단적인 전세계 뷰포트/범위 밖 좌표에는 기존 스칼라 검색으로 폴백합니다.
날짜변경선을 넘는 지도 뷰포트의 새 의미를 도입하지 않으며, min > max는 기존처럼 빈 결과입니다.
POI 반경 조회는 보수적으로 크게 잡은 MBR + 정확한 구면 반경 검사이며, 극점/날짜변경선을
넘는 반경에는 누락 방지를 위해 거리 조건만 적용합니다. 0m 조회도 지원합니다.

```sql
EXPLAIN ANALYZE
SELECT id FROM cafe
WHERE MBRIntersects(location, ST_GeomFromText(
    'MULTIPOINT((126.96 37.55),(126.98 37.57))',4326,'axis-order=long-lat'))
AND ST_Distance_Sphere(location, ST_GeomFromText(
    'POINT(126.97 37.56)',4326,'axis-order=long-lat')) <= 500;
```

작은 데이터에서는 옵티마이저가 스캔을 선택할 수 있습니다. 거리 함수만 호출한다고 공간 인덱스가
자동 적용되지는 않습니다. 공간 값은 DB에서 관리하고 JPA는 x/y와 업무 필드를 매핑합니다.
POI는 기존 Java 엔티티가 없으므로 `PoiRepository`의 JDBC count 조회를 제공하며, 테이블 이름은
`PoiType` enum만 허용합니다. 별도 Hibernate spatial 의존성이나 9개의 중복 엔티티를 추가하지 않습니다.

## 가격 의미

| 위치 | 필드 | 의미 (만원) |
|---|---|---|
| building | sale_price / jeonse_price | 건물 수준 매매가 / 전세 보증금 요약 |
| building | monthly_rent_deposit / monthly_rent | 같은 표본에서 선택한 월세 보증금과 월세 |
| building | representative_price_type / representative_price | SALE=매매가, JEONSE=보증금, MONTHLY_RENT=월세 |
| room_listing | sale_price | SALE 매물의 실제 매매가 |
| room_listing | deposit | JEONSE 또는 MONTHLY_RENT 매물의 실제 보증금 |
| room_listing | monthly_rent | MONTHLY_RENT 매물의 실제 월세 |
| building_trade | price / monthly_rent | 실거래 매매가 또는 보증금 / 월세 |

모르는 가격은 0이 아닌 NULL입니다. 대표 가격 유형과 값은 함께 NULL이거나 함께 채워져야 합니다.
월세 대표 가격을 표시할 때는 보증금도 함께 표시해야 합니다. 건물 요약은 거래 한 건이나 실제 매물을
뜻하지 않으며, 유형이 다른 대표 가격끼리 직접 정렬/비교하지 않습니다.
요약 정책(기간·면적·최저가/중앙값 등)은 아직 정하지 않아 자동 backfill하지 않습니다.
후속 ETL에서 정책과 출처를 정한 뒤 가격 필드 묶음을 같은 트랜잭션으로 갱신하세요.
기존 apt_trade는 원천 데이터로 유지하고 building_trade로의 주소 매칭/통합은 별도 작업입니다.

## 기존 DB 업그레이드

이 마이그레이션은 변경 전 저장소의 `schema.sql`(건물·사용자·커뮤니티 테이블) 기준입니다.
`room_listing`/POI를 다른 ERD로 이미 수동 생성한 DB라면 먼저 `SHOW CREATE TABLE`로 차이를 검토해야 합니다.
`CREATE TABLE IF NOT EXISTS`는 기존 테이블을 업그레이드하지 않습니다.

1. DB 백업을 만들고 Main/Auth/ETL 쓰기를 중지합니다. 별도 복제 DB에서 먼저 검증하세요.
2. 아래 결과가 0건인지 확인합니다. 잘못된 좌표는 근거 자료로 수정하며 삭제/0으로 대체하지 않습니다.

   ```sql
   SELECT id, x, y FROM building
   WHERE x IS NULL OR y IS NULL OR x <= -180 OR x > 180 OR y < -90 OR y > 90;
   ```

3. `Jachwi_in-Server-Spring`에서 마이그레이션을 **한 번만** 실행합니다.

   ```bash
   docker compose exec -T mysql sh -c 'exec mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" jachwiin_db' \
     < src/main/resources/migrations/V20260919__spatial_and_listing.sql
   ```

   기존 building ID/x/y/시설값은 보존되고 location은 기존 행에서도 계산됩니다.
   건물 유형은 ETC, 가격과 시설 갱신 시각은 NULL로 시작합니다. 새 매물/거래/POI 테이블은 비어 있습니다.
   기존 사용자/북마크/게시글 관계와 apt_trade는 유지됩니다.
4. `SHOW CREATE TABLE building`, `SHOW INDEX FROM building`과 데이터 개수를 확인한 뒤 새 서버를 시작합니다.
   `SELECT id, ST_Longitude(location), ST_Latitude(location) FROM building LIMIT 5`로 좌표도 점검합니다.

MySQL DDL은 암묵적으로 커밋되며 테이블 재구축/잠금 및 추가 디스크 공간이 필요할 수 있습니다.
이 스크립트는 Flyway 자동 실행이나 재실행 가능한 마이그레이션이 아닙니다.
중간 실패 시 적용된 테이블 상태를 확인한 뒤 남은 단계만 진행하세요. 되돌리려면 구버전 앱은 추가 컬럼을
무시하므로 먼저 앱을 롤백하고, 데이터까지 되돌려야 할 경우 검증한 백업을 복원합니다.
운영 DB/볼륨 삭제로 업그레이드를 대신하지 마세요.

## 테스트

아래 DB는 반드시 새 일회용 DB여야 합니다. 통합 테스트는 각 테스트의 쓰기를 롤백합니다.

```bash
# 저장소 루트에서 실행. 컨테이너명/포트가 이미 사용 중이면 다른 이름으로 지정하세요.
docker run -d --name jachwi-spatial-test -e MYSQL_ALLOW_EMPTY_PASSWORD=yes \
  -e MYSQL_DATABASE=jachwi_spatial_test -p 127.0.0.1:13316:3306 mysql:8.0
# mysqladmin ping이 성공하고 초기화가 끝난 뒤:
docker exec jachwi-spatial-test mysqladmin ping -uroot
docker exec -i jachwi-spatial-test mysql -uroot jachwi_spatial_test \
  < Jachwi_in-Server-Spring/src/main/resources/schema.sql
cd Jachwi_in-Server-Spring
SPATIAL_TEST_URL='jdbc:mysql://127.0.0.1:13316/jachwi_spatial_test?allowPublicKeyRetrieval=true&useSSL=false' \
  bash gradlew test bootJar --rerun-tasks
# 완료 후 이 테스트에서 생성한 컨테이너만 제거:
docker rm -fv jachwi-spatial-test
```

좌표 순서/수정 동기화, 지도 경계·0크기·전세계 범위, 모든 POI의 공간 인덱스와 반경 내외,
정확한 반경 경계·날짜변경선, 매물 유형별 가격, 잘못된 좌표를 검증합니다.
기존 DB 업그레이드는 이전 schema.sql + 기존 dummy_data.sql을 별도 DB에 넣고 마이그레이션을
적용해 새 설치 스키마 및 행 데이터와 비교할 수 있습니다. 벤치마크 프로젝트는 사용하지 않습니다.
