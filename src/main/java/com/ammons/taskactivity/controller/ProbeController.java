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
 *
 *        Modified by: Dean Ammons - March 2026 Change: Added Access-Control-Allow-Origin: *
 *        response header Reason: Portfolio page is hosted on S3 (cross-origin); without this header
 *        the browser blocks the probe fetch, incorrectly keeping dashboard links hidden on personal
 *        networks.
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
     *
     * Access-Control-Allow-Origin: * is required so that cross-origin pages (e.g. the portfolio
     * page hosted on S3) can fetch this endpoint. Without it the browser enforces CORS and the
     * fetch fails, incorrectly keeping dashboard links hidden on personal networks.
     */
    @GetMapping("/app/probe.js")
    public ResponseEntity<String> probe() {
        return ResponseEntity.ok().header("Content-Type", "application/javascript; charset=UTF-8")
                .header("Access-Control-Allow-Origin", "*")
                .body(PROBE_CONTENT);
    }
}
