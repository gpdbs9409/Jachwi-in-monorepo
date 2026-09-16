package com.capstone.Jachwi_inServerSpring.service;


import com.capstone.Jachwi_inServerSpring.domain.Building;
import com.capstone.Jachwi_inServerSpring.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor

public class MapService {
    @Autowired
    private final BuildingRepository buildingRepository;

    public List<Building> getBuildingsInArea(Double minX, Double maxX, Double minY, Double maxY) {
        return buildingRepository.findByXBetweenAndYBetween(minX, maxX, minY, maxY);
    }
}
