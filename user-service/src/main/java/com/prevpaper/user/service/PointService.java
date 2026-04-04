package com.prevpaper.user.service;

import com.prevpaper.user.entity.Account;
import com.prevpaper.user.entity.UserPointTransaction;

import java.util.List;
import java.util.UUID;

public interface PointService {

    Account addPoints(UUID authUserId, Integer points, String reason, String referenceId);
    List<UserPointTransaction> getHistory(UUID authUserId);
}
