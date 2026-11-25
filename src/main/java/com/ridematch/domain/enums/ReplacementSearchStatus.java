package com.ridematch.domain.enums;

/** Lifecycle of the short replacement search after a confirmed passenger cancels. */
public enum ReplacementSearchStatus {
    SEARCHING,
    REPLACED,
    FILLED,
    TIMED_OUT,
    CANCELLED
}
