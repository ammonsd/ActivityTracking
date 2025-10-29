package com.ammons.taskactivity.controller;

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

    // REMOVED the /login GET mapping - let Spring Security handle it
    // The custom login page is configured in SecurityConfig

    @GetMapping("/")
    public String home() {
        return "redirect:/task-activity";
    }
}
