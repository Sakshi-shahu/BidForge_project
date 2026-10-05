package com.bidforge.rfq.config;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.bidforge.rfq.entity.Rfq;
import com.bidforge.rfq.entity.RfqHistory;
import com.bidforge.rfq.repository.RfqHistoryRepository;
import com.bidforge.rfq.repository.RfqRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@EnableScheduling
public class RfqExpiryScheduler {
    private final RfqRepository rfqRepository;
    private final RfqHistoryRepository historyRepository;

    public RfqExpiryScheduler(RfqRepository rfqRepository, RfqHistoryRepository historyRepository) {
        this.rfqRepository = rfqRepository;
        this.historyRepository = historyRepository;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void closeExpiredRfqs() {
        LocalDateTime now = LocalDateTime.now();
        rfqRepository.findAll().stream()
                .filter(r -> "OPEN".equalsIgnoreCase(r.getStatus()))
                .filter(r -> r.getExpiryDateOfBid() != null && r.getExpiryDateOfBid().isBefore(now))
                .forEach(this::close);
    }

    private void close(Rfq rfq) {
        rfq.setStatus("CLOSED");
        rfqRepository.save(rfq);
        RfqHistory history = new RfqHistory();
        history.setRfq(rfq); history.setAction("AUTO_CLOSED"); history.setOldStatus("OPEN"); history.setNewStatus("CLOSED");
        history.setPreviousClosingDate(rfq.getExpiryDateOfBid()); history.setNewClosingDate(rfq.getExpiryDateOfBid());
        history.setRemarks("RFQ automatically closed after the configured closing date."); historyRepository.save(history);
    }
}
