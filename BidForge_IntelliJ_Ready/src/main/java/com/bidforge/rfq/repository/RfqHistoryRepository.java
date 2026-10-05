package com.bidforge.rfq.repository;

import com.bidforge.rfq.entity.RfqHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RfqHistoryRepository extends JpaRepository<RfqHistory, Integer> {
    List<RfqHistory> findByRfqRfqIdOrderByCreatedAtDesc(Integer rfqId);
}
