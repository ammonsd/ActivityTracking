package com.ammons.taskactivity.exception;

/**
 * ExpenseNotFoundException - Custom exception for expense not found scenarios
 *
 * @author Dean Ammons
 * @version 1.0
 */
public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(Long id) {
        super("Expense not found with ID: " + id);
    }

    public ExpenseNotFoundException(String message) {
        super(message);
    }
}
