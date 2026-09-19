package com.capstone.Jachwi_inServerSpring.domain;

/** The only allowed SQL table identifiers for POI queries. Never accept raw table names. */
public enum PoiType {
    CAFE("cafe"), CONVENIENCE_STORE("convenience_store"), HOSPITAL("hospital"),
    RESTAURANT("restaurant"), CCTV("cctv"), STREETLIGHT("streetlight"),
    SCHOOL("school"), SUBWAY_STATION("subway_station"), BUS_STOP("bus_stop");

    private final String table;
    PoiType(String table) { this.table = table; }
    public String table() { return table; }
}
