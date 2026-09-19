package com.capstone.Jachwi_inServerSpring.repository;

import com.capstone.Jachwi_inServerSpring.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

/** Run only against a disposable MySQL 8 database initialized with schema.sql; never H2. */
@DataJpaTest(properties = {
        "spring.datasource.url=${SPATIAL_TEST_URL}",
        "spring.datasource.username=${SPATIAL_TEST_USER:root}",
        "spring.datasource.password=${SPATIAL_TEST_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=none", "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "SPATIAL_TEST_URL", matches = ".+")
@Import(PoiRepository.class)
class SpatialRepositoryTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired BuildingRepository buildings;
    @Autowired RoomListingRepository listings;
    @Autowired PoiRepository pois;

    @Test void generatedPointUsesLongitudeLatitudeAndTracksUpdates() {
        jdbc.update("INSERT INTO building(x,y) VALUES (126.97,37.56)");
        assertThat(jdbc.queryForObject("SELECT ST_SRID(location) FROM building", Integer.class)).isEqualTo(4326);
        assertThat(jdbc.queryForObject("SELECT ST_Longitude(location) FROM building", Double.class)).isCloseTo(126.97, within(1e-10));
        assertThat(jdbc.queryForObject("SELECT ST_Latitude(location) FROM building", Double.class)).isCloseTo(37.56, within(1e-10));
        jdbc.update("UPDATE building SET x=127.01, y=37.57");
        assertThat(jdbc.queryForObject("SELECT ST_Longitude(location) FROM building", Double.class)).isCloseTo(127.01, within(1e-10));
        assertThat(jdbc.queryForObject("SELECT ST_Latitude(location) FROM building", Double.class)).isCloseTo(37.57, within(1e-10));
    }

    @Test void viewportIncludesEdgesAndPreservesScalarResults() {
        jdbc.update("INSERT INTO building(x,y) VALUES (126.97,37.56),(126.98,37.57),(126.975,37.565),(127.1,37.6)");
        assertThat(buildings.findByXBetweenAndYBetween(126.97,126.98,37.56,37.57))
                .extracting(Building::getId).containsExactlyInAnyOrderElementsOf(
                        buildings.findInCoordinateBounds(126.97,126.98,37.56,37.57).stream().map(Building::getId).toList());
        assertThat(buildings.findByXBetweenAndYBetween(126.97,126.97,37.56,37.56)).hasSize(1);
        assertThat(buildings.findByXBetweenAndYBetween(-180.0,180.0,-90.0,90.0)).hasSize(4);
        assertThat(buildings.findByXBetweenAndYBetween(127.0,126.0,37.0,38.0)).isEmpty();
    }

    @Test void everyPoiSupportsLegacyCoordinateInsertsAndExactRadiusFiltering() {
        for (PoiType type : PoiType.values()) {
            jdbc.update("INSERT INTO " + type.table() + "(x,y) VALUES (127,37.5),(127.001,37.5),(127.005,37.504),(128,38)");
            assertThat(pois.countWithinRadius(type,127,37.5,500)).isEqualTo(2);
            assertThat(pois.countWithinRadius(type,127,37.5,0)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name=? AND index_type='SPATIAL'", Long.class, type.table())).isEqualTo(1);
        }
    }

    @Test void radiusIncludesExactBoundaryAndHandlesDateline() {
        jdbc.update("INSERT INTO cafe(x,y) VALUES (127.001,37.5)");
        double distance=jdbc.queryForObject("SELECT ST_Distance_Sphere(location, ST_GeomFromText('POINT(127 37.5)',4326,'axis-order=long-lat')) FROM cafe",Double.class);
        assertThat(pois.countWithinRadius(PoiType.CAFE,127,37.5,distance)).isEqualTo(1);
        assertThat(pois.countWithinRadius(PoiType.CAFE,127,37.5,distance-0.001)).isZero();
        jdbc.update("INSERT INTO cafe(x,y) VALUES (-179.999,0)");
        assertThat(pois.countWithinRadius(PoiType.CAFE,179.999,0,500)).isEqualTo(1);
    }

    @Test void pricesAndListingTradeTypesRoundTripWithoutInventingLegacyPrices() {
        jdbc.update("INSERT INTO building(x,y) VALUES (127,37.5)");
        Building b=buildings.findAll().get(0);
        assertThat(b.getBuildingType()).isEqualTo(BuildingType.ETC);
        assertThat(b.getRepresentativePrice()).isNull();
        jdbc.update("INSERT INTO room_listing(building_id,room_type,trade_type,sale_price) VALUES (?,'ONE_ROOM','SALE',25000)", b.getId());
        jdbc.update("INSERT INTO room_listing(building_id,room_type,trade_type,deposit) VALUES (?,'ONE_ROOM','JEONSE',15000)", b.getId());
        jdbc.update("INSERT INTO room_listing(building_id,room_type,deposit,monthly_rent) VALUES (?,'ONE_ROOM',1000,60)", b.getId());
        assertThat(listings.findByBuildingIdAndTradeTypeAndStatus(b.getId(),TradeType.SALE,ListingStatus.AVAILABLE))
                .singleElement().satisfies(r -> assertThat(r.getSalePrice()).isEqualTo(25000L));
        assertThat(listings.findByBuildingIdAndTradeTypeAndStatus(b.getId(),TradeType.JEONSE,ListingStatus.AVAILABLE))
                .singleElement().satisfies(r -> assertThat(r.getDeposit()).isEqualTo(15000L));
        assertThat(listings.findByBuildingIdAndTradeTypeAndStatus(b.getId(),TradeType.MONTHLY_RENT,ListingStatus.AVAILABLE))
                .singleElement().satisfies(r -> assertThat(r.getMonthlyRent()).isEqualTo(60L));
    }

    @Test void buildingSummaryPricesAndTypeRoundTrip() {
        jdbc.update("""
                INSERT INTO building(x,y,building_type,sale_price,jeonse_price,monthly_rent_deposit,
                    monthly_rent,representative_price_type,representative_price)
                VALUES (127,37.5,'OFFICETEL',30000,20000,1000,65,'MONTHLY_RENT',65)
                """);
        Building b=buildings.findAll().get(0);
        assertThat(b.getBuildingType()).isEqualTo(BuildingType.OFFICETEL);
        assertThat(b.getSalePrice()).isEqualTo(30000L);
        assertThat(b.getJeonsePrice()).isEqualTo(20000L);
        assertThat(b.getMonthlyRentDeposit()).isEqualTo(1000L);
        assertThat(b.getMonthlyRent()).isEqualTo(65L);
        assertThat(b.getRepresentativePriceType()).isEqualTo(TradeType.MONTHLY_RENT);
        assertThat(b.getRepresentativePrice()).isEqualTo(65L);
    }

    @Test void rejectsInvalidCoordinatesAndNegativeRadius() {
        assertThatThrownBy(() -> pois.countWithinRadius(PoiType.CAFE,127,37.5,-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pois.countWithinRadius(PoiType.CAFE,37.5,127,500)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO building(x,y) VALUES (37.5,127)")).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
}
