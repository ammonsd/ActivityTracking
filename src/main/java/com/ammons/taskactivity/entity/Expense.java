package com.ammons.taskactivity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Expense Entity - Tracks travel and home office expenses for reimbursement
 *
 * @author Dean Ammons
 * @version 2.0
 */
@Entity
@Table(name = "expenses", schema = "public")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // User who incurred the expense
    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username cannot exceed 50 characters")
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    // Client/Project Association (like Task Activity)
    @NotBlank(message = "Client is required")
    @Size(max = 50, message = "Client cannot exceed 50 characters")
    @Column(name = "client", nullable = false, length = 50)
    private String client;

    @Size(max = 50, message = "Project cannot exceed 50 characters")
    @Column(name = "project", length = 50)
    private String project; // Optional

    // Expense Details
    @NotNull(message = "Expense date is required")
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @NotBlank(message = "Expense type is required")
    @Size(max = 50, message = "Expense type cannot exceed 50 characters")
    @Column(name = "expense_type", nullable = false, length = 50)
    private String expenseType; // From EXPENSE/EXPENSE_TYPE dropdown

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Size(max = 3, message = "Currency code must be 3 characters")
    @Column(name = "currency", length = 3)
    private String currency = "USD";

    // Payment & Vendor Information
    @NotBlank(message = "Payment method is required")
    @Size(max = 50, message = "Payment method cannot exceed 50 characters")
    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // From EXPENSE/PAYMENT_METHOD dropdown

    @Size(max = 100, message = "Vendor name cannot exceed 100 characters")
    @Column(name = "vendor", length = 100)
    private String vendor;

    @Size(max = 50, message = "Reference number cannot exceed 50 characters")
    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    // Receipt Management
    @Size(max = 500, message = "Receipt path cannot exceed 500 characters")
    @Column(name = "receipt_path", length = 500)
    private String receiptPath;

    @Size(max = 50, message = "Receipt status cannot exceed 50 characters")
    @Column(name = "receipt_status", length = 50)
    private String receiptStatus; // From EXPENSE/RECEIPT_STATUS dropdown

    // Status Tracking (CRITICAL for user visibility)
    @NotBlank(message = "Expense status is required")
    @Size(max = 50, message = "Status cannot exceed 50 characters")
    @Column(name = "expense_status", nullable = false, length = 50)
    private String expenseStatus = "Draft"; // From EXPENSE/EXPENSE_STATUS dropdown

    // Approval Details
    @Size(max = 100, message = "Approver name cannot exceed 100 characters")
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Size(max = 500, message = "Approval notes cannot exceed 500 characters")
    @Column(name = "approval_notes", length = 500)
    private String approvalNotes;

    // Reimbursement Tracking
    @DecimalMin(value = "0.00", message = "Reimbursed amount cannot be negative")
    @Column(name = "reimbursed_amount", precision = 10, scale = 2)
    private BigDecimal reimbursedAmount;

    @Column(name = "reimbursement_date")
    private LocalDate reimbursementDate;

    @Size(max = 500, message = "Reimbursement notes cannot exceed 500 characters")
    @Column(name = "reimbursement_notes", length = 500)
    private String reimbursementNotes;

    // Additional Notes
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    @Column(name = "notes", length = 500)
    private String notes;

    // Auditing
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;

    @Size(max = 100, message = "Last modified by cannot exceed 100 characters")
    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    // Constructors
    public Expense() {
        this.createdDate = LocalDateTime.now(ZoneOffset.UTC);
        this.lastModified = LocalDateTime.now(ZoneOffset.UTC);
        this.expenseStatus = "Draft";
        this.currency = "USD";
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastModified = LocalDateTime.now(ZoneOffset.UTC);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
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

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @Override
    public String toString() {
        return "Expense{" + "id=" + id + ", username='" + username + '\'' + ", expenseDate="
                + expenseDate + ", client='" + client + '\'' + ", project='" + project + '\''
                + ", expenseType='" + expenseType + '\'' + ", amount=" + amount
                + ", expenseStatus='" + expenseStatus + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Expense))
            return false;
        Expense expense = (Expense) o;
        return id != null && id.equals(expense.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
