package com.bidforge.rfq.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "FinalizedQuotations")
@Getter
@Setter
@NoArgsConstructor
public class FinalizedQuotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FinalId")
    private Integer finalId;

    @Column(name = "FinalizedDate")
    private LocalDateTime finalizedDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RFQId", nullable = false, unique = true)
    private Rfq rfq;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "QuotationId", nullable = false, unique = true)
    private RfqQuotation quotation;
}
