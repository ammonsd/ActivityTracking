package com.ammons.taskactivity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ExpenseDto - Data Transfer Object for Expense
 *
 * @author Dean Ammons
 * @version 2.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExpenseDto {

    // Expense Details
    @NotNull(message = "Expense date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expenseDate;

    @NotBlank(message = "Client is required")
    @Size(max = 50, message = "Client name cannot exceed 50 characters")
    private String client;

    @Size(max = 50, message = "Project name cannot exceed 50 characters")
    private String project; // Optional

    @NotBlank(message = "Expense type is required")
    @Size(max = 50, message = "Expense type cannot exceed 50 characters")
    private String expenseType;

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 3, message = "Currency code must be 3 characters")
    private String currency;

    // Payment Information
    @NotBlank(message = "Payment method is required")
    @Size(max = 50, message = "Payment method cannot exceed 50 characters")
    private String paymentMethod;

    @Size(max = 100, message = "Vendor name cannot exceed 100 characters")
    private String vendor;

    @Size(max = 50, message = "Reference number cannot exceed 50 characters")
    private String referenceNumber;

    // Receipt Management
    @Size(max = 500, message = "Receipt path cannot exceed 500 characters")
    private String receiptPath;

    @Size(max = 50, message = "Receipt status cannot exceed 50 characters")
    private String receiptStatus;

    // Status Tracking
    @NotBlank(message = "Expense status is required")
    @Size(max = 50, message = "Status cannot exceed 50 characters")
    private String expenseStatus;

    // Approval Details (set by manager/admin)
    @Size(max = 100, message = "Approver name cannot exceed 100 characters")
    private String approvedBy;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate approvalDate;

    @Size(max = 500, message = "Approval notes cannot exceed 500 characters")
    private String approvalNotes;

    // Reimbursement Tracking (set by admin)
    private BigDecimal reimbursedAmount;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reimbursementDate;

    @Size(max = 500, message = "Reimbursement notes cannot exceed 500 characters")
    private String reimbursementNotes;

    // Notes
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    // Username - set programmatically from logged-in user
    private String username;

    // Default constructor
    public ExpenseDto() {
        this.currency = "USD";
        this.expenseStatus = "Draft";
    }

    // Getters and Setters
    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getReceiptPath() {
        return receiptPath;
    }

    public void setReceiptPath(String receiptPath) {
        this.receiptPath = receiptPath;
    }

    public String getReceiptStatus() {
        return receiptStatus;
    }

    public void setReceiptStatus(String receiptStatus) {
        this.receiptStatus = receiptStatus;
    }

    public String getExpenseStatus() {
        return expenseStatus;
    }

    public void setExpenseStatus(String expenseStatus) {
        this.expenseStatus = expenseStatus;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDate getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDate approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getApprovalNotes() {
        return approvalNotes;
    }

    public void setApprovalNotes(String approvalNotes) {
        this.approvalNotes = approvalNotes;
    }

    public BigDecimal getReimbursedAmount() {
        return reimbursedAmount;
    }

    public void setReimbursedAmount(BigDecimal reimbursedAmount) {
        this.reimbursedAmount = reimbursedAmount;
    }

    public LocalDate getReimbursementDate() {
        return reimbursementDate;
    }

    public void setReimbursementDate(LocalDate reimbursementDate) {
        this.reimbursementDate = reimbursementDate;
    }

    public String getReimbursementNotes() {
        return reimbursementNotes;
    }

    public void setReimbursementNotes(String reimbursementNotes) {
        this.reimbursementNotes = reimbursementNotes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "ExpenseDto{" + "expenseDate=" + expenseDate + ", client='" + client + '\''
                + ", project='" + project + '\'' + ", expenseType='" + expenseType + '\''
                + ", amount=" + amount + ", expenseStatus='" + expenseStatus + '\'' + ", username='"
                + username + '\'' + '}';
    }
}
