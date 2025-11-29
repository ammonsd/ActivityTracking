package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ApiResponse;
import com.ammons.taskactivity.entity.Expense;
import com.ammons.taskactivity.service.ExpenseService;
import com.ammons.taskactivity.service.ReceiptStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for receipt upload and download
 *
 * @author Dean Ammons
 * @version 1.0
 */
@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private static final Logger logger = LoggerFactory.getLogger(ReceiptController.class);
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_TYPES =
            Arrays.asList("image/jpeg", "image/jpg", "image/png", "application/pdf");
    private static final String EXPENSE_NOT_FOUND = "Expense not found";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final ReceiptStorageService storageService;
    private final ExpenseService expenseService;

    public ReceiptController(ReceiptStorageService storageService, ExpenseService expenseService) {
        this.storageService = storageService;
        this.expenseService = expenseService;
    }

    /**
     * Upload a receipt for an expense
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @PostMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<String>> uploadReceipt(@PathVariable Long expenseId,
            @RequestParam("file") MultipartFile file, Authentication authentication) {

        String username = authentication.getName();
        logger.info("User {} uploading receipt for expense {}", username, expenseId);

        // Validate expense exists and user owns it (or is admin)
        Optional<Expense> expenseOpt = expenseService.getExpenseById(expenseId);
        if (expenseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(EXPENSE_NOT_FOUND, null));
        }

        Expense expense = expenseOpt.get();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(ROLE_ADMIN));

        if (!isAdmin && !expense.getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.error("Not authorized to upload receipt for this expense", null));
        }

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("File is empty", null));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File size exceeds maximum of 5MB", null));
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            return ResponseEntity.badRequest().body(ApiResponse
                    .error("File type not allowed. Please upload JPG, PNG, or PDF", null));
        }

        try {
            // Delete old receipt if exists
            deleteOldReceiptIfExists(expense);

            // Store new receipt
            String receiptPath = storageService.storeReceipt(file, username, expenseId);

            // Update expense with receipt path
            expense.setReceiptPath(receiptPath);
            expense.setReceiptStatus("Receipt Attached");
            expenseService.updateExpense(expenseId, convertToDto(expense));

            logger.info("Receipt uploaded successfully for expense {}: {}", expenseId, receiptPath);

            return ResponseEntity
                    .ok(ApiResponse.success("Receipt uploaded successfully", receiptPath));

        } catch (IOException e) {
            logger.error("Failed to upload receipt: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to upload receipt: " + e.getMessage(), null));
        }
    }

    /**
     * Download a receipt
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping("/{expenseId}")
    public ResponseEntity<Object> downloadReceipt(@PathVariable Long expenseId,
            Authentication authentication) {

        String username = authentication.getName();

        // Validate expense exists and user can access it
        Optional<Expense> expenseOpt = expenseService.getExpenseById(expenseId);
        if (expenseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(EXPENSE_NOT_FOUND, null));
        }

        Expense expense = expenseOpt.get();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(ROLE_ADMIN));

        if (!isAdmin && !expense.getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Not authorized to view this receipt", null));
        }

        if (expense.getReceiptPath() == null || expense.getReceiptPath().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No receipt attached to this expense", null));
        }

        try {
            InputStream receiptStream = storageService.getReceipt(expense.getReceiptPath());
            String contentType = storageService.getContentType(expense.getReceiptPath());

            // Extract filename from path
            String filename = expense.getReceiptPath()
                    .substring(expense.getReceiptPath().lastIndexOf('/') + 1);

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + filename + "\"")
                    .body(new InputStreamResource(receiptStream));

        } catch (IOException e) {
            logger.error("Failed to download receipt: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to download receipt: " + e.getMessage(), null));
        }
    }

    /**
     * Delete a receipt
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<Void>> deleteReceipt(@PathVariable Long expenseId,
            Authentication authentication) {

        String username = authentication.getName();
        logger.info("User {} deleting receipt for expense {}", username, expenseId);

        // Validate expense exists and user owns it (or is admin)
        Optional<Expense> expenseOpt = expenseService.getExpenseById(expenseId);
        if (expenseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(EXPENSE_NOT_FOUND, null));
        }

        Expense expense = expenseOpt.get();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(ROLE_ADMIN));

        if (!isAdmin && !expense.getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.error("Not authorized to delete receipt for this expense", null));
        }

        if (expense.getReceiptPath() == null || expense.getReceiptPath().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No receipt attached to this expense", null));
        }

        try {
            deleteReceiptAndUpdateExpense(expenseId, expense);
            logger.info("Receipt deleted successfully for expense {}", expenseId);
            return ResponseEntity.ok(ApiResponse.success("Receipt deleted successfully", null));

        } catch (IOException e) {
            logger.error("Failed to delete receipt: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete receipt: " + e.getMessage(), null));
        }
    }

    /**
     * Delete old receipt if it exists
     */
    private void deleteOldReceiptIfExists(Expense expense) {
        if (expense.getReceiptPath() != null && !expense.getReceiptPath().isEmpty()) {
            try {
                storageService.deleteReceipt(expense.getReceiptPath());
            } catch (IOException e) {
                logger.warn("Failed to delete old receipt: {}", e.getMessage());
            }
        }
    }

    /**
     * Delete receipt and update expense entity
     */
    private void deleteReceiptAndUpdateExpense(Long expenseId, Expense expense) throws IOException {
        storageService.deleteReceipt(expense.getReceiptPath());
        expense.setReceiptPath(null);
        expense.setReceiptStatus("Receipt Missing");
        expenseService.updateExpense(expenseId, convertToDto(expense));
    }

    // Helper method to convert Expense to ExpenseDto
    private com.ammons.taskactivity.dto.ExpenseDto convertToDto(Expense expense) {
        // Implementation similar to ExpenseService
        com.ammons.taskactivity.dto.ExpenseDto dto = new com.ammons.taskactivity.dto.ExpenseDto();
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setClient(expense.getClient());
        dto.setProject(expense.getProject());
        dto.setExpenseType(expense.getExpenseType());
        dto.setDescription(expense.getDescription());
        dto.setAmount(expense.getAmount());
        dto.setCurrency(expense.getCurrency());
        dto.setPaymentMethod(expense.getPaymentMethod());
        dto.setVendor(expense.getVendor());
        dto.setReferenceNumber(expense.getReferenceNumber());
        dto.setReceiptPath(expense.getReceiptPath());
        dto.setReceiptStatus(expense.getReceiptStatus());
        dto.setExpenseStatus(expense.getExpenseStatus());
        dto.setApprovedBy(expense.getApprovedBy());
        dto.setApprovalDate(expense.getApprovalDate());
        dto.setApprovalNotes(expense.getApprovalNotes());
        dto.setReimbursedAmount(expense.getReimbursedAmount());
        dto.setReimbursementDate(expense.getReimbursementDate());
        dto.setReimbursementNotes(expense.getReimbursementNotes());
        dto.setNotes(expense.getNotes());
        dto.setUsername(expense.getUsername());
        return dto;
    }
}
