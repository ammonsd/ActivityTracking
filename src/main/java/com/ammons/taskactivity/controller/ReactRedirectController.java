package com.ammons.taskactivity.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to redirect /dashboard and /dashboard/ to the React index.html. The resource handler
 * in ServerConfig handles all other /dashboard/** paths.
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Controller
public class ReactRedirectController {

    /**
     * Forward /dashboard and /dashboard/ to React index.html. Using forward instead of redirect
     * preserves the URL path for React Router to handle client-side routing.
     */
    @GetMapping({"/dashboard", "/dashboard/"})
    public String forwardToReact() {
        return "forward:/dashboard/index.html";
    }
}
