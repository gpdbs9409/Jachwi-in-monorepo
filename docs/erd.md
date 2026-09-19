# ERD — 구현된 스키마와 확장 테이블

기준: 서비스 테이블은 `Jachwi_in-Server-Spring/src/main/resources/schema.sql`,
전월세 원본·적재 이력·통합 뷰는 `jachwiin-scripts/load_rent.py`의 `ensure_schema()`입니다.
저장소의 스키마 정의를 표현하며, 기존 DB의 서비스 테이블은 별도 마이그레이션 적용 여부를 확인해야 합니다.
아래는 핵심 컬럼을 요약한 Mermaid ERD입니다. 전체 컬럼/제약/인덱스는 DDL이 기준입니다.
실선 관계는 DDL의 FK, `users`와 게시글/댓글/북마크의 점선 관계는 기존의 논리적 참조입니다.

```mermaid
erDiagram
    users ||..o{ posts : writes
    users ||..o{ comments : writes
    users ||..o{ bookmarks : saves
    users o|--o{ room_listing : registers
    posts ||--o{ comments : contains
    comments o|--o{ comments : replies
    building ||--o{ bookmarks : saved
    building ||--o{ room_listing : contains
    building o|--o{ building_trade : matches

    users {
        bigint id PK
        varchar email UK
        varchar password "해시"
        varchar name
        varchar nickname
        varchar school
    }
    building {
        bigint id PK
        varchar building_type "APT/OFFICETEL/MULTI_FAMILY/DETACHED_HOUSE/ETC"
        double x "경도; x,y UNIQUE"
        double y "위도"
        point location "STORED generated; NOT NULL; SRID 4326; SPATIAL INDEX"
        varchar province "DB: 시도명"
        varchar district "DB: 시군구"
        varchar neighborhood "DB: 법정읍면동명"
        varchar street_name "DB: 도로명"
        bigint sale_price "만원; nullable"
        bigint jeonse_price "만원; nullable"
        bigint monthly_rent_deposit "만원; nullable"
        bigint monthly_rent "만원; nullable"
        varchar representative_price_type "SALE/JEONSE/MONTHLY_RENT; nullable"
        bigint representative_price "만원; nullable"
        int cafe "DB: 카페; 캐시"
        int convenience_store "DB: 편의점; 캐시"
        int bus_stop "DB: 버스정류장; 캐시"
        int streetlight "DB: 가로등; 캐시"
        int cctv "DB: CCTV; 캐시"
        int hospital "DB: 병원; 캐시"
        int restaurant "DB: 식당; 캐시"
        double school_distance "DB: 학교_거리"
        datetime amenity_updated_at "nullable"
    }
    room_listing {
        bigint id PK
        bigint building_id FK
        bigint user_id FK "nullable"
        varchar room_type
        varchar trade_type "SALE/JEONSE/MONTHLY_RENT"
        bigint deposit "전세 또는 월세 보증금; 만원"
        bigint monthly_rent "월세; 만원"
        bigint sale_price "매매가; 만원"
        bigint maintenance_fee "만원"
        int floor
        double area "제곱미터"
        text photos "JSON 배열 문자열"
        varchar status "AVAILABLE/RESERVED/CLOSED"
        datetime created_at
        datetime updated_at
    }
    building_trade {
        bigint id PK
        bigint building_id FK "매칭 전 nullable"
        varchar trade_type "SALE/JEONSE/MONTHLY_RENT"
        double area
        varchar floor
        date deal_date
        bigint price "매매가 또는 보증금; 만원"
        bigint monthly_rent "만원"
        varchar source
        datetime created_at
    }
    posts {
        bigint id PK
        bigint user_id "논리적 참조"
        varchar title
        text content
        varchar category
        int view_count
    }
    comments {
        bigint id PK
        bigint post_id FK
        bigint user_id "논리적 참조"
        bigint parent_id FK "nullable"
        text content
    }
    bookmarks {
        bigint id PK
        bigint user_id "building_id와 복합 UNIQUE"
        bigint building_id FK
    }
```

## POI (공간 관계, building FK 없음)

POI는 여러 건물에서 공유하는 위치 데이터입니다. 특정 건물의 자식 행이 아니며 반경으로 연결합니다.
각 테이블에 `id PK`, `name`, `x`, `y`, `location`, `source`, `created_at`을 둡니다.
`name`은 CCTV/가로등 등을 위해 nullable입니다. location은 모두 생성 컬럼 + SRID 4326 공간 인덱스입니다.

```mermaid
erDiagram
    cafe {
        bigint id PK
        double x
        double y
        point location "SRID 4326; SPATIAL"
    }
    convenience_store {
        bigint id PK
        double x
        double y
        point location "SRID 4326; SPATIAL"
    }
    hospital {
        bigint id PK
        double x
        double y
        point location "SRID 4326; SPATIAL"
    }
    restaurant {
        bigint id PK
        double x
        double y
        point location "SRID 4326; SPATIAL"
    }
    cctv {
        bigint id PK
        double x
        double y
        point location "SRID 4326; SPATIAL"
    }
    streetlight {
        bigint id PK
        double x
        double y
        point location "SRID 4326; SPATIAL"
    }
    school {
        bigint id PK
        varchar school_level
        double x
        double y
        point location "SRID 4326; SPATIAL"
    }
    subway_station {
        bigint id PK
        varchar line
        double x
        double y
        point location "SRID 4326; SPATIAL"
    }
    bus_stop {
        bigint id PK
        varchar stop_id
        varchar routes
        double x
        double y
        point location "SRID 4326; SPATIAL"
    }
```

건물 시설 카운트와 `amenity_updated_at`은 캐시이며 자동 계산 트리거/배치는 아직 없습니다.
현재 추천은 기존 캐시값을 사용하고 학교 거리의 기존 의미도 바꾸지 않습니다.
지하철역·학교 POI를 추가했다고 자동으로 사용자별 거리 추천이 활성화되지는 않습니다.

기존 `apt_trade`는 별도 원천 테이블로 `AptTrade` 엔티티와 적재 스크립트가 계속 사용합니다.
그 테이블의 DDL은 `jachwiin-scripts/load_apt_csv.py` / `load_apt_trade.py`에 있으며
`building_trade`로 자동 이전하지 않습니다. 건물 유형은 building이 소유합니다.
이전 기획 ERD의 building_review/user_preference/chat_session/chat_message는 현재 DDL/API에
구현되어 있지 않아 이 문서에서 구현 완료로 표시하지 않습니다.

## 전월세 실거래 원본과 적재 이력

원천 API별로 같은 구조의 테이블을 사용합니다. 원본 실거래는 건물 주소 매칭 전 데이터이므로
`building_id` FK가 없습니다. 아래 독립 테이블 사이에도 물리적 FK는 없습니다.
`rental_import_log`는 `(source, region_code, deal_ym)` 단위의 마지막 성공 적재를 기록합니다.

```mermaid
erDiagram
    raw_multi_family_rent {
        bigint id PK "스냅샷 행 번호; 재적재 시 변경 가능"
        char region_code "5자리 시군구 코드; 복합 UK 구성"
        char deal_ym "YYYYMM; 복합 UK 구성"
        char record_hash "SHA-256; 복합 UK 구성"
        int occurrence_no "동일 원본 출현 순번; 복합 UK 구성"
        varchar building_type "MULTI_FAMILY"
        varchar trade_type "JEONSE/MONTHLY_RENT"
        varchar dong
        varchar building_name
        varchar jibun
        decimal area "DECIMAL(14,4); 제곱미터"
        varchar floor
        int built_year
        date deal_date
        bigint deposit "보증금; 만원"
        bigint monthly_rent "월세; 만원; 0이면 전세"
        varchar contract_type
        varchar contract_term
        json raw_payload "API item 전체"
        datetime fetched_at "UTC; DATETIME(6)"
    }
    raw_officetel_rent {
        bigint id PK "스냅샷 행 번호; 재적재 시 변경 가능"
        char region_code "5자리 시군구 코드; 복합 UK 구성"
        char deal_ym "YYYYMM; 복합 UK 구성"
        char record_hash "SHA-256; 복합 UK 구성"
        int occurrence_no "동일 원본 출현 순번; 복합 UK 구성"
        varchar building_type "OFFICETEL"
        varchar trade_type "JEONSE/MONTHLY_RENT"
        varchar dong
        varchar building_name
        varchar jibun
        decimal area "DECIMAL(14,4); 제곱미터"
        varchar floor
        int built_year
        date deal_date
        bigint deposit "보증금; 만원"
        bigint monthly_rent "월세; 만원; 0이면 전세"
        varchar contract_type
        varchar contract_term
        json raw_payload "API item 전체"
        datetime fetched_at "UTC; DATETIME(6)"
    }
    raw_apt_rent {
        bigint id PK "스냅샷 행 번호; 재적재 시 변경 가능"
        char region_code "5자리 시군구 코드; 복합 UK 구성"
        char deal_ym "YYYYMM; 복합 UK 구성"
        char record_hash "SHA-256; 복합 UK 구성"
        int occurrence_no "동일 원본 출현 순번; 복합 UK 구성"
        varchar building_type "APT"
        varchar trade_type "JEONSE/MONTHLY_RENT"
        varchar dong
        varchar building_name
        varchar jibun
        decimal area "DECIMAL(14,4); 제곱미터"
        varchar floor
        int built_year
        date deal_date
        bigint deposit "보증금; 만원"
        bigint monthly_rent "월세; 만원; 0이면 전세"
        varchar contract_type
        varchar contract_term
        json raw_payload "API item 전체"
        datetime fetched_at "UTC; DATETIME(6)"
    }
    rental_import_log {
        varchar source PK "multi_family/officetel/apt; 복합 PK"
        char region_code PK "복합 PK"
        char deal_ym PK "복합 PK"
        int row_count "완전한 월별 스냅샷의 행 수"
        datetime fetched_at "UTC; DATETIME(6)"
    }
```

원본 테이블 각각의 인덱스는 다음과 같습니다.

| 인덱스 | 컬럼 | 의미 |
|---|---|---|
| PRIMARY KEY | id | 해당 스냅샷 행 번호 |
| uk_snapshot_record (UNIQUE) | region_code, deal_ym, record_hash, occurrence_no | 동일 공개 내용의 별개 계약도 출현 횟수만큼 보존 |
| idx_rent_region_date | region_code, deal_date | 지역·계약일 조회 |
| idx_rent_type | trade_type | 전세/월세 조회 |

적재기는 원천·지역·월별 데이터를 하나의 트랜잭션으로 교체하고 적재 이력을 갱신합니다.
재실행 시 행 수가 누적되지 않으며, 원본 `id`는 외부 FK나 영속 거래 식별자로 사용하지 않습니다.

## 통합 조회 뷰와 데이터 흐름

`rental_trade`는 세 원본을 `UNION ALL`로 조회하는 **뷰**입니다. 별도 저장 테이블이나 FK가 없으며,
`(source, source_row_id)`는 현재 스냅샷에서 원본 행을 찾는 조합일 뿐 DB의 PK 제약이 아닙니다.
`record_hash`, `occurrence_no`, `raw_payload`는 원본 테이블에만 있습니다.

```mermaid
erDiagram
    rental_trade {
        varchar source "원천 구분; 뷰에서 추가"
        bigint source_row_id "원본 id의 별칭; FK 아님"
        char region_code
        char deal_ym
        varchar building_type
        varchar trade_type
        varchar dong
        varchar building_name
        varchar jibun
        decimal area
        varchar floor
        int built_year
        date deal_date
        bigint deposit "만원"
        bigint monthly_rent "만원"
        varchar contract_type
        varchar contract_term
        datetime fetched_at "UTC"
    }
```

아래 화살표는 데이터 처리 흐름이며 FK 관계가 아닙니다. 점선은 아직 구현하지 않은 후속 단계입니다.

```mermaid
flowchart LR
    RH[연립다세대 전월세 API] --> RHR[raw_multi_family_rent]
    OFFI[오피스텔 전월세 API] --> OFFIR[raw_officetel_rent]
    APT[아파트 전월세 API] --> APTR[raw_apt_rent]
    RHR -->|UNION ALL| VIEW[rental_trade 뷰]
    OFFIR -->|UNION ALL| VIEW
    APTR -->|UNION ALL| VIEW
    LOADER[load_rent.py] --> LOG[rental_import_log]
    VIEW -. 주소 검증 및 건물 매칭 .-> MATCH[후속 ETL]
    MATCH -. building_id 연결 .-> TRADE[building_trade]
    MATCH -. 유형 및 가격 요약 갱신 .-> BUILDING[building]
```

공공 실거래 원본은 과거 계약 기록입니다. 현재 구할 수 있는 매물인 `room_listing`과 구분하며,
원본 적재만으로 매물 생성·건물 매칭·건물 가격 요약 갱신을 수행하지 않습니다.
원본의 `building_type`은 API 종류를 나타내고, 건물 마스터의 유형은 검증된 매칭 이후 반영합니다.
상세한 수집 범위, 재실행 방식과 조회 예시는 [전월세 적재 안내](rental-import.md)를 참고하세요.
