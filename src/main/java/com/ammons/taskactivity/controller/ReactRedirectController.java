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
     * Redirect /dashboard and /dashboard/ to /dashboard/index.html. The trailing slash pattern
     * ensures both /dashboard and /dashboard/ are handled.
     */
    @GetMapping({"/dashboard", "/dashboard/"})
    public String redirectToReact() {
        return "redirect:/dashboard/index.html";
    }
}
