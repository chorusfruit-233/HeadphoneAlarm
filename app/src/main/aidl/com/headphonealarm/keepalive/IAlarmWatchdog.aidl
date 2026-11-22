package com.headphonealarm.keepalive;

interface IAlarmWatchdog {
    void observe(IBinder appLifetime, IBinder shizukuBackend) = 1;
    // Shizuku's UserService cleanup transaction.
    void destroy() = 16777114;
}
