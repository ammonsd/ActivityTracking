package com.ammons.taskactivity.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Basic Security Integration Tests for Task Activity Controllers Tests authentication requirements
 * and basic authorization
 * 
 * @author Dean Ammons
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test that authenticated users can access basic endpoints
     */
    @Test
    @WithMockUser(roles = "USER")
    void testAuthenticatedUserAccess() throws Exception {
        // The /task-activity endpoint redirects to /task-activity/add by design
        mockMvc.perform(get("/task-activity")).andExpect(status().is3xxRedirection());
    }

    /**
     * Test that admin users can access admin endpoints
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminUserAccess() throws Exception {
        // The /task-activity endpoint redirects to /task-activity/add by design
        mockMvc.perform(get("/task-activity")).andExpect(status().is3xxRedirection());
    }
}
