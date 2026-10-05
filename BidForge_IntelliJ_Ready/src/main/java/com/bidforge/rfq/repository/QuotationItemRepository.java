package com.bidforge.rfq.repository;

import com.bidforge.rfq.entity.QuotationItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotationItemRepository extends JpaRepository<QuotationItem, Integer> {
    void deleteByQuotationQuotationId(Integer quotationId);
}
