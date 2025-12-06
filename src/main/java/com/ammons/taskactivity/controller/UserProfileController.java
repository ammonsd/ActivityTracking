package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.UserEditDto;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * UserProfileController - Handles profile editing for non-admin users
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Controller
@RequestMapping("/profile")
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'EXPENSE_ADMIN')")
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);
    private static final String USERNAME = "username";
    private static final String USER_NOT_FOUND = "User not found";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String SUCCESS_MESSAGE = "successMessage";
    private static final String ADMIN_USER_EDIT = "admin/user-edit";
    private static final String IS_OWN_PROFILE = "isOwnProfile";

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Show the edit profile form for current user
     */
    @GetMapping("/edit")
    public String showEditProfileForm(Model model, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        String currentUsername = authentication.getName();
        logger.info("User {} accessing their own profile edit form", currentUsername);

        Optional<User> userOptional = userService.getUserByUsername(currentUsername);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, USER_NOT_FOUND);
            return "redirect:/task-activity/list";
        }

        User user = userOptional.get();
        UserEditDto userEditDto = new UserEditDto(user.getId(), user.getUsername(), user.getRole(),
                user.isEnabled(), user.isForcePasswordUpdate());
        userEditDto.setFirstname(user.getFirstname());
        userEditDto.setLastname(user.getLastname());
        userEditDto.setCompany(user.getCompany());
        userEditDto.setEmail(user.getEmail());
        userEditDto.setAccountLocked(user.isAccountLocked());
        userEditDto.setFailedLoginAttempts(user.getFailedLoginAttempts());

        model.addAttribute("userEditDto", userEditDto);
        model.addAttribute(IS_OWN_PROFILE, true);
        addUserDisplayInfo(model, authentication);

        return ADMIN_USER_EDIT;
    }

    /**
     * Process the edit profile form submission for current user
     */
    @PostMapping("/edit")
    public String editProfile(@Valid @ModelAttribute UserEditDto userEditDto,
            BindingResult bindingResult, Model model, Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String currentUsername = authentication.getName();
        logger.info("User {} attempting to update their own profile", currentUsername);

        if (bindingResult.hasErrors()) {
            model.addAttribute(IS_OWN_PROFILE, true);
            addUserDisplayInfo(model, authentication);
            return ADMIN_USER_EDIT;
        }

        try {
            Optional<User> userOptional = userService.getUserByUsername(currentUsername);
            if (userOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, USER_NOT_FOUND);
                return "redirect:/profile/edit";
            }

            User user = userOptional.get();

            // Users can only update their own basic profile information
            // Role, enabled status, and security settings can only be changed by ADMIN
            user.setFirstname(userEditDto.getFirstname());
            user.setLastname(userEditDto.getLastname());
            user.setCompany(userEditDto.getCompany());
            user.setEmail(userEditDto.getEmail());

            User updatedUser = userService.updateUser(user);
            logger.info("User {} successfully updated their profile", updatedUser.getUsername());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Profile updated successfully");
            return "redirect:/profile/edit";
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update profile: {}", e.getMessage());
            bindingResult.rejectValue(USERNAME, "error.username", e.getMessage());
            model.addAttribute(IS_OWN_PROFILE, true);
            addUserDisplayInfo(model, authentication);
            return ADMIN_USER_EDIT;
        }
    }

    /**
     * Add user display information to the model
     */
    private void addUserDisplayInfo(Model model, Authentication authentication) {
        if (authentication != null) {
            String username = authentication.getName();
            model.addAttribute(USERNAME, username);

            // Fetch user details to display full name
            userService.getUserByUsername(username).ifPresent(user -> {
                String firstname = user.getFirstname() != null ? user.getFirstname() : "";
                String lastname = user.getLastname() != null ? user.getLastname() : "";
                String displayName = (firstname + " " + lastname + " (" + username + ")").trim();
                displayName = displayName.replaceAll("\\s+", " ");
                model.addAttribute("userDisplayName", displayName);
            });
        }
    }
}
