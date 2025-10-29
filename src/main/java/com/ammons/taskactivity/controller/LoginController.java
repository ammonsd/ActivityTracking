package com.ammons.taskactivity.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * LoginController - Simple controller to serve the login page Spring Security handles the POST
 * processing
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        // Simply return the login view name
        // Spring Security will handle redirects after successful authentication
        return "login";
    }
}
