package com.bidforge.rfq.repository;

import com.bidforge.rfq.entity.RfqVendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RfqVendorRepository extends JpaRepository<RfqVendor, Integer> {
    List<RfqVendor> findByVendorUserIdOrderByRfqRfqIdDesc(Integer vendorId);
    List<RfqVendor> findByRfqRfqId(Integer rfqId);
    boolean existsByRfqRfqIdAndVendorUserId(Integer rfqId, Integer vendorId);
    void deleteByRfqRfqId(Integer rfqId);
}
