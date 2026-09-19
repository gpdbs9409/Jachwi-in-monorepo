# 전월세 실거래 적재

`jachwiin-scripts/load_rent.py`는 국토교통부의 다음 세 API를 사용합니다.

| 원천 | API 서비스 / 메서드 | 적재 테이블 | building_type |
|---|---|---|---|
| 연립다세대 | RTMSDataSvcRHRent / getRTMSDataSvcRHRent | raw_multi_family_rent | MULTI_FAMILY |
| 오피스텔 | RTMSDataSvcOffiRent / getRTMSDataSvcOffiRent | raw_officetel_rent | OFFICETEL |
| 아파트 | RTMSDataSvcAptRent / getRTMSDataSvcAptRent | raw_apt_rent | APT |

각 서비스의 기본 URL은 `https://apis.data.go.kr/1613000/서비스명`입니다.
요청은 시군구 법정동 코드 `LAWD_CD` 5자리와 계약년월 `DEAL_YMD` 6자리 기준입니다.
인증키는 `MOLIT_API_KEY` 환경변수로만 전달합니다. 인코딩된 키와 디코딩된 키를 모두 지원하며,
requests가 query string을 인코딩하기 전에 한 번 디코딩하여 `%` 이중 인코딩을 막습니다.
키가 포함된 URL/HTTP 예외 원문은 로그에 출력하지 않습니다.

## 원본과 통합 조회

세 테이블은 공통 컬럼으로 날짜, 주소 단서, 건물명, 유형, 면적, 보증금/월세(만원),
계약 유형·기간을 보존합니다. 전체 API item은 `raw_payload JSON`에 함께 저장합니다.
월세가 명시적으로 `0`이면 JEONSE, 양수면 MONTHLY_RENT입니다. 월세 누락/비정상 가격은
전세로 추측하지 않고 해당 월 적재를 실패 처리합니다.

통합 조회는 `rental_trade` 뷰에서 `UNION ALL`로 제공합니다.
조회 예:

```sql
SELECT building_type, trade_type, COUNT(*) AS contracts,
       MIN(deal_date) AS first_date, MAX(deal_date) AS last_date
FROM rental_trade
GROUP BY building_type, trade_type;

SELECT region_code, building_name, dong, jibun, deal_date, deposit, monthly_rent
FROM rental_trade
WHERE building_type = 'OFFICETEL' AND trade_type = 'MONTHLY_RENT'
ORDER BY deal_date DESC LIMIT 20;
```

이 데이터는 **실거래 이력**이며 현재 판매/임대 중인 매물이 아닙니다.
`room_listing`이나 `building`을 생성하지 않고, 좌표도 임의로 채우지 않습니다.
주소/지번 검증 후 building에 연결하고 `building_trade`로 통합하는 ETL은 후속 단계입니다.
기존 apt_trade·building·커뮤니티 테이블은 수정하지 않습니다.

## 실행

Python 3.9 이상에서 `pip install -r jachwiin-scripts/requirements.txt`로 의존성을 설치합니다.
`jachwiin-scripts/.env.example`의 이름에 맞춰 환경변수를 셸에 설정하세요.
이 스크립트는 `.env`를 자동으로 읽지 않습니다. 비밀번호/인증키를 명령행 인수로 넘기지 마세요.

```bash
python3 jachwiin-scripts/load_rent.py \
  --regions 11200,11215,11410,11440 --start 202501 --end 202609 \
  --cache-dir /absolute/path/to/rental-cache \
  --report /absolute/path/to/import-report.json
```

`--regions seoul`은 서울 25개 구, `--sources multi_family,officetel,apt`는 선택한 원천입니다.
지역/기간에는 숨겨진 기본값이 없습니다. `--download-only`는 DB 연결 없이 다운로드/검증만 수행하고,
`--offline`은 저장된 캐시만 DB에 적재합니다. API 응답 성공 코드, 페이지 번호, totalCount,
요청한 지역/계약년월을 검증하고 완전한 월별 응답만 원자적으로 캐시에 저장합니다.

실패한 범위는 최종 JSON의 `failed`에 기록하며 하나라도 실패하면 종료 코드 1입니다.
보고서의 `rows`는 완료된 범위의 최종 행 수이며 신규 증가량이 아닙니다.
`previous_rows`로 교체 전 행 수를 확인할 수 있습니다.
실패 범위만 `--regions`, `--sources`, `--start`, `--end`로 좁혀 재시도하세요.
API 일일 호출 한도는 계정/서비스 승인 조건을 확인하세요. HTTP 429/5xx와 연결 오류는 제한적으로 재시도합니다.

## 재실행과 정정 데이터

전월세 API에는 모든 유형에 공통된 공개 계약 ID가 없습니다.
같은 날짜·건물·층·면적·가격의 두 계약도 실제로 별개일 수 있어 단순 DISTINCT로 제거하지 않습니다.
전체 공개 item의 SHA-256 + 동일 item의 출현 순번을 저장하여 이러한 중복 표본을 보존합니다.

적재 단위는 원천/시군구/계약년월입니다. **해당 원본 테이블의 그 범위만** 완전한 새 스냅샷으로 교체합니다.
DELETE와 INSERT, `rental_import_log` 갱신은 같은 트랜잭션이며 MySQL advisory lock으로
같은 범위의 동시 실행을 막습니다. 다운로드/파싱 실패 시 기존 데이터는 보존됩니다.
유효한 0건 응답은 이전 해당 월 데이터를 0건으로 교체합니다.
한 달보다 큰 전체 실행은 단일 트랜잭션이 아니므로 완료/실패 보고서를 확인하세요.

현재 `id`는 적재마다 달라질 수 있는 원본 스냅샷 행 번호입니다.
외부 FK를 이 ID에 연결하지 마세요. 향후 영속 거래 ID/주소 매칭 체계를 정한 뒤 이력을 통합해야 합니다.
재실행은 동일 계약을 계속 추가하지 않으며 API 정정·삭제도 반영하지만 원본의 모든 과거 버전을
DB에 보관하는 방식은 아닙니다. 변경 전 스냅샷 보존이 필요하면 캐시/DB를 백업하세요.

## 테스트

```bash
PYTHONPATH=jachwiin-scripts python3 -m unittest discover -s jachwiin-scripts/tests -v
```

기본 실행은 파싱/API 모의 테스트만 실행합니다. DB 테스트는 일회용 MySQL의
`rent_import_test` DB와 `RENT_TEST_PORT`가 필요합니다. 실제 DB를 테스트에 지정하지 마세요.
테스트는 테스트용 원본 테이블을 비우므로 포트와 DB 이름을 반드시 확인합니다.

```bash
docker run -d --name jachwi-rent-test -e MYSQL_ALLOW_EMPTY_PASSWORD=yes \
  -e MYSQL_DATABASE=rent_import_test -p 127.0.0.1:13316:3306 mysql:8.0
# 초기화 완료 후
RENT_TEST_PORT=13316 PYTHONPATH=jachwiin-scripts \
  python3 -m unittest discover -s jachwiin-scripts/tests -v
docker rm -fv jachwi-rent-test
```

검증 범위: 전세/월세 판별, 숫자/날짜 정규화, API 페이지 완전성, 비밀정보 없는 오류,
동일 공개 계약 보존, 재실행 건수 불변, 파싱/DB 쓰기 실패 시 원래 데이터 보존,
세 원천의 통합 조회, 월별 범위 격리.
