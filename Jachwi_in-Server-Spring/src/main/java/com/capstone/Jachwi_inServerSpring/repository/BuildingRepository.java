package com.capstone.Jachwi_inServerSpring.repository;

import com.capstone.Jachwi_inServerSpring.domain.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BuildingRepository extends JpaRepository<Building, Long> {

    // MULTIPOINT defines an MBR without geographic polygon edge curvature.
    // MBRIntersects includes boundary points, just like the old BETWEEN query.
    @Query(value = """
            SELECT b.* FROM building b
            WHERE MBRIntersects(b.location, ST_GeomFromText(CONCAT(
                'MULTIPOINT((', :minX, ' ', :minY, '),(', :maxX, ' ', :maxY, '))'),
                4326, 'axis-order=long-lat'))
              AND b.x BETWEEN :minX AND :maxX AND b.y BETWEEN :minY AND :maxY
            """, nativeQuery = true)
    List<Building> findInSpatialBounds(@Param("minX") Double minX, @Param("maxX") Double maxX,
                                            @Param("minY") Double minY, @Param("maxY") Double maxY);

    // Preserve scalar BETWEEN semantics for empty, out-of-domain or global viewports.
    // Geographic MBRs use a wrapping longitude interval and are unsuitable for these cases.
    default List<Building> findByXBetweenAndYBetween(Double minX, Double maxX, Double minY, Double maxY) {
        if (minX == null || maxX == null || minY == null || maxY == null
                || minX > maxX || minY > maxY) return List.of();
        if (minX > -180 && maxX <= 180 && maxX - minX < 180 && minY >= -90 && maxY <= 90) {
            return findInSpatialBounds(minX, maxX, minY, maxY);
        }
        return findInCoordinateBounds(minX, maxX, minY, maxY);
    }

    @Query("SELECT b FROM Building b WHERE b.x BETWEEN :minX AND :maxX AND b.y BETWEEN :minY AND :maxY")
    List<Building> findInCoordinateBounds(@Param("minX") Double minX, @Param("maxX") Double maxX,
                                          @Param("minY") Double minY, @Param("maxY") Double maxY);
}
