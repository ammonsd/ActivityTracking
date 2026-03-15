package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

/**
 * Description: Controller for the public Account Setup Request form. Serves the request form page
 * and processes submissions by emailing the details to the system administrator. No authentication
 * is required and no data is persisted to the database.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since March 2026
 */
@Controller
public class AccountSetupRequestController {

    private static final Logger logger =
            LoggerFactory.getLogger(AccountSetupRequestController.class);

    private static final Set<String> VALID_TRACKING_TYPES = Set.of("TASK", "EXPENSE", "BOTH");

    private final EmailService emailService;

    public AccountSetupRequestController(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Renders the account setup request form.
     *
     * @return the Thymeleaf view name
     */
    @GetMapping("/request-account")
    public String showRequestForm() {
        return "request-account";
    }

    /**
     * Processes the account setup request submission. Validates input, sends the provided details
     * to the configured administrator email, and redirects with a success or error status
     * parameter. The trackingType must be one of: TASK, EXPENSE, or BOTH.
     *
     * @param firstName the applicant's first name
     * @param lastName the applicant's last name
     * @param email the applicant's email address
     * @param company the applicant's company name (optional)
     * @param trackingType the requested tracking type: TASK, EXPENSE, or BOTH
     * @return redirect to the request form with a status query parameter
     */
    @PostMapping("/request-account")
    public String submitRequest(@RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName, @RequestParam("email") String email,
            @RequestParam(value = "company", defaultValue = "") String company,
            @RequestParam("trackingType") String trackingType) {

        firstName = firstName.trim();
        lastName = lastName.trim();
        email = email.trim();
        company = company.trim();
        trackingType = trackingType.trim().toUpperCase();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            return "redirect:/request-account?error=missing";
        }

        // Basic email format check to prevent injections via email header
        if (!email.matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            return "redirect:/request-account?error=invalid_email";
        }

        // Enforce allow-list for trackingType to prevent injection in email body
        if (!VALID_TRACKING_TYPES.contains(trackingType)) {
            return "redirect:/request-account?error=invalid_type";
        }

        try {
            emailService.sendAccountSetupRequest(firstName, lastName, email, company, trackingType);
            logger.info("Account setup request submitted for: {} {}", firstName, lastName);
            return "redirect:/request-account?success";
        } catch (Exception e) {
            logger.error("Failed to process account setup request from {}: {}", email,
                    e.getMessage(), e);
            return "redirect:/request-account?error=send_failed";
        }
    }
}
