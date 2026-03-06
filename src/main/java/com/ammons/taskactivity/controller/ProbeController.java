package com.ammons.taskactivity.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Description: Serves a lightweight probe endpoint at /app/probe.js that the sidebar uses to detect
 * corporate proxy (Zscaler) JS blocking. Dashboard links are hidden by default and revealed only
 * when this endpoint returns a genuine application/javascript response. The Angular build wipes
 * static/app/ on every compile, so this must be a controller endpoint rather than a static file.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since March 2026
 */
@Controller
public class ProbeController {

    private static final String PROBE_CONTENT =
            "/* probe - dashboard JS asset availability check */\n";

    /**
     * Returns a minimal JavaScript response used by the client-side Zscaler probe. Accessible
     * without authentication (covered by /app/*.js in SecurityConfig permitAll). Returns
     * Content-Type: application/javascript so the client probe can distinguish a real JS response
     * from a Zscaler block page (which returns text/html).
     */
    @GetMapping("/app/probe.js")
    public ResponseEntity<String> probe() {
        return ResponseEntity.ok().header("Content-Type", "application/javascript; charset=UTF-8")
                .body(PROBE_CONTENT);
    }
}
