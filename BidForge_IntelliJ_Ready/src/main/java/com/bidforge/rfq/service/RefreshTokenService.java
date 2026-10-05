package com.bidforge.rfq.service;

import com.bidforge.rfq.entity.User;

public interface RefreshTokenService {
    void createOrReplace(User user);
    boolean isUsable(User user);
    void revoke(User user);
}
