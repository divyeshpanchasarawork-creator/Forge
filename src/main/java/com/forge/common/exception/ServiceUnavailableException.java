package com.forge.common.exception;

/**
 * An upstream dependency (e.g. the LeetCode GraphQL API) failed or returned unusable data.
 * Maps to HTTP 502 Bad Gateway so callers can retry without treating it as their fault.
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
