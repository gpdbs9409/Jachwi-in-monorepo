package com.capstone.Jachwi_inServerSpring.repository;

import com.capstone.Jachwi_inServerSpring.domain.RoomListing;
import com.capstone.Jachwi_inServerSpring.domain.TradeType;
import com.capstone.Jachwi_inServerSpring.domain.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomListingRepository extends JpaRepository<RoomListing, Long> {
    List<RoomListing> findByBuildingIdAndTradeTypeAndStatus(Long buildingId, TradeType tradeType, ListingStatus status);
}
