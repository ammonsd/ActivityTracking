package com.ammons.taskactivity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VisitorCounterService.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
class VisitorCounterServiceTest {

    private VisitorCounterService service;

    @BeforeEach
    void setUp() {
        service = new VisitorCounterService();
    }

    @Test
    void testIncrementAndGet() {
        String pageName = "test-page";

        assertEquals(1, service.incrementAndGet(pageName));
        assertEquals(2, service.incrementAndGet(pageName));
        assertEquals(3, service.incrementAndGet(pageName));
    }

    @Test
    void testGetCount() {
        String pageName = "test-page";

        assertEquals(0, service.getCount(pageName));
        service.incrementAndGet(pageName);
        assertEquals(1, service.getCount(pageName));
    }

    @Test
    void testMultiplePages() {
        service.incrementAndGet("page1");
        service.incrementAndGet("page1");
        service.incrementAndGet("page2");

        assertEquals(2, service.getCount("page1"));
        assertEquals(1, service.getCount("page2"));
    }

    @Test
    void testResetCount() {
        String pageName = "test-page";

        service.incrementAndGet(pageName);
        service.incrementAndGet(pageName);
        assertEquals(2, service.getCount(pageName));

        service.resetCount(pageName);
        assertEquals(0, service.getCount(pageName));
    }

    @Test
    void testResetAllCounts() {
        service.incrementAndGet("page1");
        service.incrementAndGet("page2");

        service.resetAllCounts();

        assertEquals(0, service.getCount("page1"));
        assertEquals(0, service.getCount("page2"));
    }

    @Test
    void testPageNameSanitization() {
        assertEquals(1, service.incrementAndGet("Test Page!"));
        assertEquals(2, service.incrementAndGet("TEST-PAGE-"));
        assertEquals(3, service.incrementAndGet("test page!"));
    }
}
