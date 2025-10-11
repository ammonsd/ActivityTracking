package com.ammons.taskactivity.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * AuthController
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(Authentication authentication) {
        // Redirect to main page if already authenticated
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/task-activity";
        }
        return "login";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/task-activity";
    }
}
