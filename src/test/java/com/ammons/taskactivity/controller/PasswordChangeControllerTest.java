package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.config.TestSecurityConfig;
import com.ammons.taskactivity.entity.Role;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.service.UserService;
import com.ammons.taskactivity.repository.UserRepository;
import com.ammons.taskactivity.repository.TaskActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PasswordChangeController Tests password change functionality for both voluntary
 * and forced updates
 *
 * @author Dean Ammons
 */
@WebMvcTest(PasswordChangeController.class)
@Import(TestSecurityConfig.class)
@DisplayName("PasswordChangeController Tests")
class PasswordChangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TaskActivityRepository taskActivityRepository;

    @MockitoBean
    private com.ammons.taskactivity.security.JwtUtil jwtUtil;

    @MockitoBean
    private com.ammons.taskactivity.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setFirstname("Test");
        testUser.setLastname("User");
        testUser.setRole(Role.USER);
        testUser.setForcePasswordUpdate(false);

        session = new MockHttpSession();
    }

    @Nested
    @DisplayName("Display Password Change Form Tests")
    class DisplayPasswordChangeFormTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should display password change form for authenticated user")
        void shouldDisplayPasswordChangeFormForAuthenticatedUser() throws Exception {
            when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));

            mockMvc.perform(get("/change-password")).andExpect(status().isOk())
                    .andExpect(view().name("change-password"))
                    .andExpect(model().attributeExists("passwordChangeDto"))
                    .andExpect(model().attribute("isForced", false))
                    .andExpect(model().attributeExists("userDisplayName"));

            verify(userService, times(1)).getUserByUsername("testuser");
        }

        @Test
        @DisplayName("Should return 401 when not authenticated (WebMvcTest behavior)")
        void shouldRedirectToLoginWhenNotAuthenticated() throws Exception {
            // In @WebMvcTest, unauthenticated requests return 401
            // In full app context with security, this would redirect to /login
            mockMvc.perform(get("/change-password")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should show forced password update when forced parameter is true")
        void shouldShowForcedPasswordUpdateWhenForcedParameterIsTrue() throws Exception {
            when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));

            mockMvc.perform(get("/change-password").param("forced", "true"))
                    .andExpect(status().isOk()).andExpect(view().name("change-password"))
                    .andExpect(model().attribute("isForced", true));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should show forced password update when session attribute is set")
        void shouldShowForcedPasswordUpdateWhenSessionAttributeIsSet() throws Exception {
            when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
            session.setAttribute("requiresPasswordUpdate", true);

            mockMvc.perform(get("/change-password").session(session)).andExpect(status().isOk())
                    .andExpect(view().name("change-password"))
                    .andExpect(model().attribute("isForced", true));
        }
    }

    @Nested
    @DisplayName("Process Password Change Tests")
    class ProcessPasswordChangeTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should change password successfully")
        void shouldChangePasswordSuccessfully() throws Exception {
            when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
            doNothing().when(userService).changePassword("testuser", "NewPass123!", true);

            mockMvc.perform(post("/change-password").with(csrf()).param("username", "testuser")
                    .param("newPassword", "NewPass123!").param("confirmNewPassword", "NewPass123!"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/task-activity/list")).andExpect(flash().attribute(
                            "successMessage", containsString("Password changed successfully")));

            verify(userService, times(1)).changePassword("testuser", "NewPass123!", true);
        }

        @Test
        @DisplayName("Should return 401 when not authenticated (WebMvcTest behavior)")
        void shouldRedirectToLoginWhenNotAuthenticated() throws Exception {
            // In @WebMvcTest, unauthenticated requests return 401
            // In full app context with security, this would redirect to /login
            mockMvc.perform(post("/change-password").with(csrf()).param("username", "testuser")
                    .param("newPassword", "NewPass123!").param("confirmNewPassword", "NewPass123!"))
                    .andExpect(status().isUnauthorized());

            verify(userService, never()).changePassword(anyString(), anyString(), anyBoolean());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should reject when passwords don't match")
        void shouldRejectWhenPasswordsDontMatch() throws Exception {
            when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));

            mockMvc.perform(post("/change-password").with(csrf()).param("username", "testuser")
                    .param("newPassword", "NewPass123!")
                    .param("confirmNewPassword", "DifferentPass123!")).andExpect(status().isOk())
                    .andExpect(view().name("change-password"))
                    .andExpect(model().attribute("errorMessage", "New passwords do not match"))
                    .andExpect(model().attribute("isForced", true));

            verify(userService, never()).changePassword(anyString(), anyString(), anyBoolean());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should show validation errors for invalid password")
        void shouldShowValidationErrorsForInvalidPassword() throws Exception {
            when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));

            mockMvc.perform(post("/change-password").with(csrf()).param("username", "testuser")
                    .param("newPassword", "weak") // Too weak
                    .param("confirmNewPassword", "weak")).andExpect(status().isOk())
                    .andExpect(view().name("change-password")).andExpect(model().hasErrors());

            verify(userService, never()).changePassword(anyString(), anyString(), anyBoolean());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should handle password change service exception")
        void shouldHandlePasswordChangeServiceException() throws Exception {
            when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
            doThrow(new IllegalArgumentException("Password does not meet requirements"))
                    .when(userService).changePassword("testuser", "NewPass123!", true);

            mockMvc.perform(post("/change-password").with(csrf()).param("username", "testuser")
                    .param("newPassword", "NewPass123!").param("confirmNewPassword", "NewPass123!"))
                    .andExpect(status().isOk()).andExpect(view().name("change-password"))
                    .andExpect(model().attribute("errorMessage",
                            containsString("Failed to change password")))
                    .andExpect(model().attribute("isForced", true));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should remove session attribute after successful password change")
        void shouldRemoveSessionAttributeAfterSuccessfulPasswordChange() throws Exception {
            when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));
            doNothing().when(userService).changePassword("testuser", "NewPass123!", true);
            session.setAttribute("requiresPasswordUpdate", true);

            mockMvc.perform(post("/change-password").with(csrf()).session(session)
                    .param("username", "testuser").param("newPassword", "NewPass123!")
                    .param("confirmNewPassword", "NewPass123!"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/task-activity/list"));

            // Session attribute should be removed
            assert session.getAttribute("requiresPasswordUpdate") == null;
        }
    }

    @Nested
    @DisplayName("User Display Information Tests")
    class UserDisplayInformationTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should display correct user information")
        void shouldDisplayCorrectUserInformation() throws Exception {
            when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(testUser));

            mockMvc.perform(get("/change-password")).andExpect(status().isOk())
                    .andExpect(model().attribute("userDisplayName", containsString("Test User")));
        }

        @Test
        @WithMockUser(username = "unknownuser")
        @DisplayName("Should handle missing user gracefully")
        void shouldHandleMissingUserGracefully() throws Exception {
            when(userService.getUserByUsername("unknownuser")).thenReturn(Optional.empty());

            mockMvc.perform(get("/change-password")).andExpect(status().isOk())
                    .andExpect(view().name("change-password"));
        }
    }
}
