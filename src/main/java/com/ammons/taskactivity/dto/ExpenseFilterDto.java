package com.ammons.taskactivity.dto;

import java.time.LocalDate;

/**
 * DTO for filtering expenses with multiple criteria. Reduces method parameter count and improves
 * maintainability.
 *
 * @author Dean Ammons
 * @version 1.0
 */
public class ExpenseFilterDto {

    private String username;
    private String client;
    private String project;
    private String expenseType;
    private String status;
    private String paymentMethod;
    private LocalDate startDate;
    private LocalDate endDate;

    // Constructors
    public ExpenseFilterDto() {}

    public ExpenseFilterDto(String username, String client, String project, String expenseType,
            String status, String paymentMethod, LocalDate startDate, LocalDate endDate) {
        this.username = username;
        this.client = client;
        this.project = project;
        this.expenseType = expenseType;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
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

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
