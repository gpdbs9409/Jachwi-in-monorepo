package com.capstone.Jachwi_inServerSpring.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "room_listing")
public class RoomListing {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "building_id", nullable = false)
    private Long buildingId;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "room_type", nullable = false, length = 30)
    private String roomType;
    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false, columnDefinition = "varchar(20)")
    private TradeType tradeType = TradeType.MONTHLY_RENT;
    private Long deposit;
    @Column(name = "monthly_rent")
    private Long monthlyRent;
    @Column(name = "sale_price")
    private Long salePrice;
    @Column(name = "maintenance_fee")
    private Long maintenanceFee;
    private Integer floor;
    private Double area;
    @Column(columnDefinition = "TEXT")
    private String photos;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private ListingStatus status = ListingStatus.AVAILABLE;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
