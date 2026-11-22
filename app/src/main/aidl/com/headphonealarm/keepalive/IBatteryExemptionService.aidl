package com.headphonealarm.keepalive;

interface IBatteryExemptionService {
    boolean setExempt(boolean enabled) = 1;
    // Shizuku's UserService cleanup transaction.
    void destroy() = 16777114;
}
