package com.ammons.taskactivity.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to redirect /app and /app/ to the Angular index.html. The resource handler in
 * ServerConfig handles all other /app/** paths.
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Controller
public class AngularRedirectController {

    /**
     * Redirect /app and /app/ to /app/index.html. The trailing slash pattern ensures both /app and
     * /app/ are handled.
     */
    @GetMapping({"/app", "/app/"})
    public String redirectToAngular() {
        return "redirect:/app/index.html";
    }
}
