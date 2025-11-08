package com.ammons.taskactivity.config;

import org.springframework.security.core.AuthenticationException;

/**
 * Exception thrown when a GUEST user attempts to authenticate with an expired password. GUEST users
 * cannot change their own passwords, so they must contact a system administrator.
 *
 * @author Dean Ammons
 * @version 1.0
 */
public class GuestPasswordExpiredException extends AuthenticationException {

    public GuestPasswordExpiredException(String message) {
        super(message);
    }

    public GuestPasswordExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
