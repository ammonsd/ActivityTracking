package com.ammons.taskactivity.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to forward /dashboard and /dashboard/ to the React index.html. The resource handler in
 * ServerConfig handles all other /dashboard/** paths.
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Controller
public class ReactRedirectController {

    /**
     * Forward /dashboard and /dashboard/ to /dashboard/index.html. This keeps the URL as /dashboard
     * in the browser while serving the index.html file.
     */
    @GetMapping({"/dashboard", "/dashboard/"})
    public String redirectToReact() {
        return "forward:/dashboard/index.html";
    }
}
