package com.capstone.Jachwi_inServerSpring.service;

import com.capstone.Jachwi_inServerSpring.domain.AptTrade;
import com.capstone.Jachwi_inServerSpring.repository.AptTradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AptTradeService {

    private final AptTradeRepository aptTradeRepository;

    public Page<AptTrade> getTrades(String sigungu, String dong, String dealYm, @NonNull Pageable pageable) {
        if (sigungu != null && dong != null && dealYm != null) {
            return aptTradeRepository.findBySigunguAndDongAndDealYm(sigungu, dong, dealYm, pageable);
        } else if (sigungu != null && dong != null) {
            return aptTradeRepository.findBySigunguAndDong(sigungu, dong, pageable);
        } else if (sigungu != null && dealYm != null) {
            return aptTradeRepository.findBySigunguAndDealYm(sigungu, dealYm, pageable);
        } else if (sigungu != null) {
            return aptTradeRepository.findBySigungu(sigungu, pageable);
        }
        return aptTradeRepository.findAll(pageable);
    }
}
