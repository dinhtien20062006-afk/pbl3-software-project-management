package com.pbl3.entity;

public enum UserStatus {
    ACTIVE,
    BUSY,
    INACTIVE,
    BANNED;

    public boolean canLogin() {
        return this == ACTIVE;
    }

    public boolean canTransitionTo(UserStatus nextStatus) {
        if (this == BANNED) return false; 
        return true;
    }
}
