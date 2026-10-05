package com.bidforge.rfq.service;

import com.bidforge.rfq.dto.LoginRequest;
import com.bidforge.rfq.dto.LoginResponse;
import com.bidforge.rfq.dto.OtpRequest;
import com.bidforge.rfq.dto.QrSetupResponse;
import com.bidforge.rfq.dto.VerifyLoginOtpRequest;
import com.bidforge.rfq.entity.User;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    String ACCESS_COOKIE = "accessToken";
    String RECOVERY_FLOW_COOKIE = "recoveryFlow";
    String RECOVERY_SETUP_FLOW_COOKIE = "recoverySetupFlow";

    LoginResponse login(LoginRequest request, HttpServletResponse response);

    QrSetupResponse getSetupQr(LoginRequest request);

    void verifyLoginOtp(VerifyLoginOtpRequest request, HttpServletResponse response);

    void refreshAccessToken(String accessToken, HttpServletResponse response);

    void logout(String accessToken, HttpServletResponse response);

    String startLostOtp(LoginRequest request, HttpServletResponse response);

    String verifyLostEmailOtp(OtpRequest request, String recoveryFlow, HttpServletResponse response);

    QrSetupResponse getRecoverySetupQr(String recoverySetupFlow);

    void verifyRecoveryOtp(OtpRequest request, String recoverySetupFlow, HttpServletResponse response);

    User register(String name, String email);
}
