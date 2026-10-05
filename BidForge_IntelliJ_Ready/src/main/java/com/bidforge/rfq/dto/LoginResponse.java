package com.bidforge.rfq.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private boolean success;
    private String message;
    private String next;
    private String email;
    private String qrDataUri;
    private String secret;
}
