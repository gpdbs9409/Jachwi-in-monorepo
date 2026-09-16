package com.capstone.Jachwi_inServerSpring.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "apt_trade", schema = "jachwiin_db")
public class AptTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sigungu")
    private String sigungu;

    @Column(name = "dong")
    private String dong;

    @Column(name = "apt_name")
    private String aptName;

    @Column(name = "area")
    private java.math.BigDecimal area;

    @Column(name = "floor")
    private String floor;

    @Column(name = "built_year")
    private Integer builtYear;

    @Column(name = "deal_ym")
    private String dealYm;

    @Column(name = "deal_day")
    private String dealDay;

    @Column(name = "deal_type")
    private String dealType;

    @Column(name = "price")
    private Integer price;

    @Column(name = "jibun")
    private String jibun;

    @Column(name = "bon_bun")
    private String bonBun;

    @Column(name = "bu_bun")
    private String buBun;

    @Column(name = "road_addr")
    private String roadAddr;

    @Column(name = "agent_region")
    private String agentRegion;

    @Column(name = "buyer")
    private String buyer;

    @Column(name = "seller")
    private String seller;

    @Column(name = "cancel_date")
    private String cancelDate;

    @Column(name = "reg_date")
    private String regDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
