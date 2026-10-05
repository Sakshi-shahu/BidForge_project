package com.bidforge.rfq.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rfq_history", indexes = {
        @Index(name = "idx_rfq_history_rfq", columnList = "rfq_id")
})
@Getter
@Setter
@NoArgsConstructor
public class RfqHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(length = 30)
    private String oldStatus;

    @Column(length = 30)
    private String newStatus;

    private LocalDateTime previousClosingDate;
    private LocalDateTime newClosingDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(length = 500)
    private String remarks;
}
