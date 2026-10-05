package com.bidforge.rfq.exception;

public class InvalidOtpException extends AuthenticationException {

    public InvalidOtpException(String message) {
        super(message);
    }
}
