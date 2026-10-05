package com.bidforge.rfq.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "RFQs")
@Getter
@Setter
@NoArgsConstructor
public class Rfq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RFQId")
    private Integer rfqId;

    @Column(name = "RFQNo", nullable = false)
    private String rfqNo;

    @Column(name = "IndentNo", nullable = false)
    private String indentNo;

    @Column(name = "RFQLineNo")
    private String rfqLineNo;

    @Column(name = "ItemNo")
    private String itemNo;

    @Column(name = "ItemName")
    private String itemName;

    @Column(name = "ReqQty")
    private Integer reqQty;

    @Column(name = "Description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "DeliveryLocation")
    private String deliveryLocation;

    @Column(name = "UOM")
    private String uom;

    @Column(name = "ReqDeliveryDate")
    private LocalDateTime reqDeliveryDate;

    @Column(name = "BidDate")
    private LocalDateTime bidDate;

    @Column(name = "ExpiryDateofBid")
    private LocalDateTime expiryDateOfBid;

    @Column(name = "Mobile")
    private String mobile;

    @Column(name = "ContactPerson")
    private String contactPerson;

    @Column(name = "Status")
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    private User createdBy;


    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RfqItem> items = new ArrayList<>();
}
