package com.bidforge.rfq.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QrSetupResponse {
    private String qrDataUri;
    private String secret;
}
