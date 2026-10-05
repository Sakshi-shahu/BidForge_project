package com.bidforge.rfq.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RfqDraft {
    private Integer rfqId;
    private String rfqNo;
    private String indentNo;
    private String mobile;
    private String contactPerson;
    private LocalDate bidStartDate;
    private LocalDate bidEndDate;
    private List<Integer> vendorIds = new ArrayList<>();
    private List<RfqItemDraft> items = new ArrayList<>();
}
