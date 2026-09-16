package com.capstone.Jachwi_inServerSpring.repository;

import com.capstone.Jachwi_inServerSpring.domain.AptTrade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AptTradeRepository extends JpaRepository<AptTrade, Long> {

    Page<AptTrade> findBySigungu(String sigungu, Pageable pageable);

    Page<AptTrade> findBySigunguAndDong(String sigungu, String dong, Pageable pageable);

    Page<AptTrade> findByAptName(String aptName, Pageable pageable);

    Page<AptTrade> findBySigunguAndDealYm(String sigungu, String dealYm, Pageable pageable);

    Page<AptTrade> findBySigunguAndDongAndDealYm(String sigungu, String dong, String dealYm, Pageable pageable);
}
