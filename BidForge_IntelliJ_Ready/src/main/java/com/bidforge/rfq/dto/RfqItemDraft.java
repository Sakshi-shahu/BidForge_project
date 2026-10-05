package com.bidforge.rfq.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class RfqItemDraft {
    private String lineNo;
    private String itemNo;
    private String factoryCode;
    private String itemName;
    private Integer requiredQuantity;
    private String uom;
    private LocalDate deliveryDate;
    private String deliveryLocation;
    private String description;
}
