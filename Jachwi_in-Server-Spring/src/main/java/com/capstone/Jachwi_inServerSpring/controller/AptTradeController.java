package com.capstone.Jachwi_inServerSpring.controller;

import com.capstone.Jachwi_inServerSpring.domain.AptTrade;
import com.capstone.Jachwi_inServerSpring.service.AptTradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/apt-trade")
public class AptTradeController {

    private final AptTradeService aptTradeService;

    @GetMapping
    public ResponseEntity<Page<AptTrade>> getTrades(
            @RequestParam(required = false) String sigungu,
            @RequestParam(required = false) String dong,
            @RequestParam(required = false) String dealYm,
            @PageableDefault(size = 20) @org.springframework.lang.NonNull Pageable pageable) {
        return ResponseEntity.ok(aptTradeService.getTrades(sigungu, dong, dealYm, pageable));
    }
}
