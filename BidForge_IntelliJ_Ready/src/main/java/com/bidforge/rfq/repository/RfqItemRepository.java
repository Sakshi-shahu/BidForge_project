package com.bidforge.rfq.repository;

import com.bidforge.rfq.entity.RfqItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RfqItemRepository extends JpaRepository<RfqItem, Integer> {
}
