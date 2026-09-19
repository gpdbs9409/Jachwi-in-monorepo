package com.capstone.Jachwi_inServerSpring.repository;

import com.capstone.Jachwi_inServerSpring.domain.PoiType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PoiRepository {
    private final JdbcTemplate jdbc;

    /** Count within a spherical radius in metres, after a conservative indexed MBR filter. */
    public long countWithinRadius(PoiType type, double longitude, double latitude, double radiusMetres) {
        if (type == null || !Double.isFinite(longitude) || !Double.isFinite(latitude)
                || !Double.isFinite(radiusMetres) || longitude <= -180 || longitude > 180
                || latitude < -90 || latitude > 90 || radiusMetres < 0) {
            throw new IllegalArgumentException("Valid WGS84 coordinates and a non-negative radius are required");
        }
        String point = "POINT(" + longitude + " " + latitude + ")";
        String distance = "ST_Distance_Sphere(location, ST_GeomFromText(?, 4326, 'axis-order=long-lat')) <= ?";
        // Smaller than WGS84 mean radius: the prefilter is deliberately larger than the search circle.
        double angle = radiusMetres / 6_350_000.0;
        double latitudeDelta = Math.toDegrees(angle);
        double minY = latitude - latitudeDelta;
        double maxY = latitude + latitudeDelta;
        if (minY > -90 && maxY < 90) {
            double longitudeDelta = Math.toDegrees(Math.asin(Math.sin(angle) / Math.cos(Math.toRadians(latitude))));
            double minX = longitude - longitudeDelta;
            double maxX = longitude + longitudeDelta;
            if (minX > -180 && maxX <= 180 && maxX - minX < 180) {
                String bounds = "MULTIPOINT((" + minX + " " + minY + "),(" + maxX + " " + maxY + "))";
                return jdbc.queryForObject("SELECT COUNT(*) FROM " + type.table()
                        + " WHERE MBRIntersects(location, ST_GeomFromText(?, 4326, 'axis-order=long-lat')) AND "
                        + distance, Long.class, bounds, point, radiusMetres);
            }
        }
        // A single non-wrapping MBR cannot safely describe a pole/dateline crossing circle.
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + type.table() + " WHERE " + distance,
                Long.class, point, radiusMetres);
    }
}
