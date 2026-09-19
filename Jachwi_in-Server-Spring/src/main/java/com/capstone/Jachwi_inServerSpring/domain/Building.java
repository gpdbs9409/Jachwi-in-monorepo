package com.capstone.Jachwi_inServerSpring.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "building", indexes = {
        @Index(name = "uk_building_xy", columnList = "x, y", unique = true)
})
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 좌표 (경도, 위도)
    @Column(nullable = false)
    private Double x;

    @Column(nullable = false)
    private Double y;

    @Enumerated(EnumType.STRING)
    @Column(name = "building_type", nullable = false, columnDefinition = "varchar(30)")
    private BuildingType buildingType = BuildingType.ETC;

    @Column(name = "sale_price")
    private Long salePrice;
    @Column(name = "jeonse_price")
    private Long jeonsePrice;
    @Column(name = "monthly_rent_deposit")
    private Long monthlyRentDeposit;
    @Column(name = "monthly_rent")
    private Long monthlyRent;
    @Enumerated(EnumType.STRING)
    @Column(name = "representative_price_type", columnDefinition = "varchar(20)")
    private TradeType representativePriceType;
    @Column(name = "representative_price")
    private Long representativePrice;
    @Column(name = "amenity_updated_at")
    private java.time.LocalDateTime amenityUpdatedAt;

    // location is a DB-generated, indexed POINT. x/y remain the API/write contract.
    // 주소 정보 - 기존 DB 컬럼명(한글) 유지
    @Column(name = "시도명")
    private String province;

    @Column(name = "시군구")
    private String district;

    @Column(name = "법정읍면동명")
    private String neighborhood;

    @Column(name = "도로명")
    private String streetName;

    @Column(name = "건물본번")
    private Integer buildingMainNo;

    @Column(name = "건물부번")
    private Integer buildingSubNo;

    @Column(name = "건축물대장_건물명")
    private String officialBuildingName;

    @Column(name = "상세건물명")
    private String detailBuildingName;

    @Column(name = "시군구용_건물명")
    private String localBuildingName;

    // 주변 시설 수
    @Column(name = "버스정류장")
    private Integer busStop;

    @Column(name = "편의점")
    private Integer convenienceStore;

    @Column(name = "카페")
    private Integer cafe;

    @Column(name = "가로등")
    private Integer streetLight;

    @Column(name = "CCTV")
    private Integer cctv;

    @Column(name = "병원")
    private Integer hospital;

    @Column(name = "식당")
    private Integer restaurant;

    // 학교까지의 거리 (미터)
    @Column(name = "학교_거리")
    private Double schoolDistance;
}
