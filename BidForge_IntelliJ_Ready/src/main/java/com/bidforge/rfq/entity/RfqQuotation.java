package com.bidforge.rfq.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "RFQQuotations")
@Getter
@Setter
@NoArgsConstructor
public class RfqQuotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "QuotationId")
    private Integer quotationId;

    @Column(name = "BidNo")
    private String bidNo;

    @Column(name = "QuotedAmount", precision = 18, scale = 4)
    private BigDecimal quotedAmount;

    @Column(name = "DeliveryDate")
    private LocalDateTime deliveryDate;

    @Column(name = "PaymentTerms")
    private String paymentTerms;

    @Column(name = "Remarks", columnDefinition = "LONGTEXT")
    private String remarks;

    @Column(name = "Status")
    private String status;

    @Column(name = "SubmittedDate")
    private LocalDateTime submittedDate;

    @Column(name = "InvoiceGenerated", nullable = false)
    private boolean invoiceGenerated = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RFQId", nullable = false)
    private Rfq rfq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VendorId", nullable = false)
    private User vendor;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuotationItem> items = new ArrayList<>();
}
