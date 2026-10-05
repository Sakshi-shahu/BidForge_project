package com.bidforge.rfq.repository;

import com.bidforge.rfq.entity.FinalizedQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinalizedQuotationRepository extends JpaRepository<FinalizedQuotation, Integer> {
    List<FinalizedQuotation> findAllByOrderByFinalIdDesc();
    Optional<FinalizedQuotation> findByRfqRfqId(Integer rfqId);
}
