package com.bidforge.rfq.repository;

import com.bidforge.rfq.entity.Rfq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RfqRepository extends JpaRepository<Rfq, Integer> {
    List<Rfq> findAllByOrderByRfqIdDesc();
    List<Rfq> findByCreatedByUserIdOrderByRfqIdDesc(Integer userId);
}
