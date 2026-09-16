package com.capstone.Jachwi_inServerSpring.controller;

import com.capstone.Jachwi_inServerSpring.domain.Building;
import com.capstone.Jachwi_inServerSpring.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/map")
public class MapController {

    private final MapService mapService;

    @GetMapping("/view/position")
    public ResponseEntity<List<Building>> postPosition(
            @RequestParam double minX,
            @RequestParam double maxX,
            @RequestParam double minY,
            @RequestParam double maxY){
        List<Building> buildings = mapService.getBuildingsInArea(minX, maxX, minY, maxY);
        return ResponseEntity.ok(buildings);
    }
}