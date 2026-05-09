package com.example.business;

public interface AuditLogger {
    void log(String event, String detail);
}
