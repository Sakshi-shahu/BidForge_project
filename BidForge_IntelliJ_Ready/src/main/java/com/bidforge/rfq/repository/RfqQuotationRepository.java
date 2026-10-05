package com.bidforge.rfq.repository;

import com.bidforge.rfq.entity.RfqQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RfqQuotationRepository extends JpaRepository<RfqQuotation, Integer> {
    List<RfqQuotation> findByRfqRfqIdOrderByQuotationIdDesc(Integer rfqId);
    List<RfqQuotation> findByVendorUserIdOrderByQuotationIdDesc(Integer vendorId);
    Optional<RfqQuotation> findByRfqRfqIdAndVendorUserId(Integer rfqId, Integer vendorId);
}
