package com.bidforge.rfq.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "RFQItem")
@Getter
@Setter
@NoArgsConstructor
public class RfqItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ItemId")
    private Integer itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RFQId", nullable = false)
    private Rfq rfq;

    @Column(name = "RFQLineNo", nullable = false)
    private String rfqLineNo;

    @Column(name = "ItemNo", nullable = false)
    private String itemNo;

    @Column(name = "FactoryCode")
    private String factoryCode;

    @Column(name = "ItemName", nullable = false)
    private String itemName;

    @Column(name = "ReqQty", nullable = false)
    private Integer reqQty;

    @Column(name = "UOM", nullable = false)
    private String uom;

    @Column(name = "ReqDeliveryDate", nullable = false)
    private LocalDateTime reqDeliveryDate;

    @Column(name = "DeliveryLocation")
    private String deliveryLocation;

    @Column(name = "Description", columnDefinition = "LONGTEXT")
    private String description;

}
