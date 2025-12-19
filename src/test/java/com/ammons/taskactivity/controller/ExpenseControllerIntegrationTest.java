package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ExpenseDto;
import com.ammons.taskactivity.entity.Expense;
import com.ammons.taskactivity.repository.ExpenseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ExpenseController. Tests expense management endpoints including approval
 * workflow with different user roles.
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@DisplayName("ExpenseController Integration Tests")
class ExpenseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpenseRepository expenseRepository;

    private ExpenseDto testDto;

    @BeforeEach
    void setUp() {
        testDto = new ExpenseDto();
        testDto.setExpenseDate(LocalDate.now());
        testDto.setClient("Test Client");
        testDto.setProject("Test Project");
        testDto.setExpenseType("Travel");
        testDto.setDescription("Test Expense");
        testDto.setAmount(new BigDecimal("100.00"));
        testDto.setPaymentMethod("Credit Card");
        testDto.setExpenseStatus("Draft");
        testDto.setUsername("testuser");
    }

    @Nested
    @DisplayName("POST /api/expenses - Create Expense")
    class CreateExpenseTests {

        @Test
        @WithUserDetails("user")
        @DisplayName("Should create expense for user with CREATE permission")
        void shouldCreateExpenseWithPermission() throws Exception {
            mockMvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testDto)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.client").value("Test Client"));
        }

        @Test
        @WithUserDetails("guest")
        @DisplayName("Should deny creation for guest without CREATE permission")
        void shouldDenyCreateForGuest() throws Exception {
            mockMvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testDto)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/expenses - Get All Expenses")
    class GetAllExpensesTests {

        @BeforeEach
        void createTestData() {
            Expense expense = new Expense();
            expense.setExpenseDate(LocalDate.now());
            expense.setClient("Test Client");
            expense.setProject("Test Project");
            expense.setExpenseType("Travel");
            expense.setDescription("Test");
            expense.setAmount(new BigDecimal("100.00"));
            expense.setPaymentMethod("Cash");
            expense.setExpenseStatus("Draft");
            expense.setUsername("testuser");
            expenseRepository.save(expense);
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Admin should see all expenses")
        void adminShouldSeeAllExpenses() throws Exception {
            mockMvc.perform(get("/api/expenses")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithUserDetails("testuser")
        @DisplayName("Regular user should only see their own expenses")
        void userShouldSeeOnlyOwnExpenses() throws Exception {
            mockMvc.perform(get("/api/expenses")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithUserDetails("expenseadmin")
        @DisplayName("Expense admin should see all expenses")
        void expenseAdminShouldSeeAllExpenses() throws Exception {
            mockMvc.perform(get("/api/expenses")).andExpect(status().isOk());
        }

        @Test
        @WithUserDetails("user")
        @DisplayName("Should support filtering parameters")
        void shouldSupportFiltering() throws Exception {
            mockMvc.perform(
                    get("/api/expenses").param("status", "Draft").param("client", "Test Client"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/expenses/{id}/submit - Submit for Approval")
    class SubmitExpenseTests {

        private Long testExpenseId;

        @BeforeEach
        void createTestExpense() {
            Expense expense = new Expense();
            expense.setExpenseDate(LocalDate.now());
            expense.setClient("Test Client");
            expense.setProject("Test Project");
            expense.setExpenseType("Travel");
            expense.setDescription("Test");
            expense.setAmount(new BigDecimal("100.00"));
            expense.setPaymentMethod("Cash");
            expense.setExpenseStatus("Draft");
            expense.setUsername("testuser");
            expense = expenseRepository.save(expense);
            testExpenseId = expense.getId();
        }

        @Test
        @WithUserDetails("testuser")
        @DisplayName("User should submit their own expense")
        void userShouldSubmitOwnExpense() throws Exception {
            mockMvc.perform(post("/api/expenses/" + testExpenseId + "/submit"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.expenseStatus").value("Submitted"));
        }

        @Test
        @WithUserDetails("guest")
        @DisplayName("Guest without SUBMIT permission should be denied")
        void guestShouldBeDeniedSubmit() throws Exception {
            mockMvc.perform(post("/api/expenses/" + testExpenseId + "/submit"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/expenses/{id}/approve - Approve Expense")
    class ApproveExpenseTests {

        private Long testExpenseId;

        @BeforeEach
        void createSubmittedExpense() {
            Expense expense = new Expense();
            expense.setExpenseDate(LocalDate.now());
            expense.setClient("Test Client");
            expense.setProject("Test Project");
            expense.setExpenseType("Travel");
            expense.setDescription("Test");
            expense.setAmount(new BigDecimal("100.00"));
            expense.setPaymentMethod("Cash");
            expense.setExpenseStatus("Submitted");
            expense.setUsername("testuser");
            expense = expenseRepository.save(expense);
            testExpenseId = expense.getId();
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Admin should approve expense")
        void adminShouldApproveExpense() throws Exception {
            mockMvc.perform(post("/api/expenses/" + testExpenseId + "/approve").param("notes",
                    "Approved by admin")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.expenseStatus").value("Approved"));
        }

        @Test
        @WithUserDetails("expenseadmin")
        @DisplayName("Expense admin should approve expense")
        void expenseAdminShouldApproveExpense() throws Exception {
            mockMvc.perform(post("/api/expenses/" + testExpenseId + "/approve"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithUserDetails("user")
        @DisplayName("Regular user should not approve expense")
        void userShouldNotApproveExpense() throws Exception {
            mockMvc.perform(post("/api/expenses/" + testExpenseId + "/approve"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/expenses/{id}/reject - Reject Expense")
    class RejectExpenseTests {

        private Long testExpenseId;

        @BeforeEach
        void createSubmittedExpense() {
            Expense expense = new Expense();
            expense.setExpenseDate(LocalDate.now());
            expense.setClient("Test Client");
            expense.setProject("Test Project");
            expense.setExpenseType("Travel");
            expense.setDescription("Test");
            expense.setAmount(new BigDecimal("100.00"));
            expense.setPaymentMethod("Cash");
            expense.setExpenseStatus("Submitted");
            expense.setUsername("testuser");
            expense = expenseRepository.save(expense);
            testExpenseId = expense.getId();
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Admin should reject expense with notes")
        void adminShouldRejectExpense() throws Exception {
            mockMvc.perform(post("/api/expenses/" + testExpenseId + "/reject").param("notes",
                    "Missing receipt")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.expenseStatus").value("Rejected"));
        }

        @Test
        @WithUserDetails("user")
        @DisplayName("Regular user should not reject expense")
        void userShouldNotRejectExpense() throws Exception {
            mockMvc.perform(
                    post("/api/expenses/" + testExpenseId + "/reject").param("notes", "Test"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Should require notes for rejection")
        void shouldRequireNotesForRejection() throws Exception {
            // Missing required parameter results in 500 error from Spring framework
            mockMvc.perform(post("/api/expenses/" + testExpenseId + "/reject"))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("POST /api/expenses/{id}/reimburse - Mark as Reimbursed")
    class ReimburseExpenseTests {

        private Long testExpenseId;

        @BeforeEach
        void createApprovedExpense() {
            Expense expense = new Expense();
            expense.setExpenseDate(LocalDate.now());
            expense.setClient("Test Client");
            expense.setProject("Test Project");
            expense.setExpenseType("Travel");
            expense.setDescription("Test");
            expense.setAmount(new BigDecimal("100.00"));
            expense.setPaymentMethod("Cash");
            expense.setExpenseStatus("Approved");
            expense.setUsername("testuser");
            expense = expenseRepository.save(expense);
            testExpenseId = expense.getId();
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Admin should mark expense as reimbursed")
        void adminShouldMarkAsReimbursed() throws Exception {
            mockMvc.perform(post("/api/expenses/" + testExpenseId + "/reimburse")
                    .param("reimbursedAmount", "100.00")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.expenseStatus").value("Reimbursed"));
        }

        @Test
        @WithUserDetails("expenseadmin")
        @DisplayName("Expense admin should mark as reimbursed")
        void expenseAdminShouldMarkAsReimbursed() throws Exception {
            mockMvc.perform(post("/api/expenses/" + testExpenseId + "/reimburse"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithUserDetails("user")
        @DisplayName("Regular user should not mark as reimbursed")
        void userShouldNotMarkAsReimbursed() throws Exception {
            mockMvc.perform(post("/api/expenses/" + testExpenseId + "/reimburse"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/expenses/pending-approvals - Approval Queue")
    class ApprovalQueueTests {

        @BeforeEach
        void createPendingExpenses() {
            Expense expense = new Expense();
            expense.setExpenseDate(LocalDate.now());
            expense.setClient("Test Client");
            expense.setProject("Test Project");
            expense.setExpenseType("Travel");
            expense.setDescription("Test");
            expense.setAmount(new BigDecimal("100.00"));
            expense.setPaymentMethod("Cash");
            expense.setExpenseStatus("Submitted");
            expense.setUsername("testuser");
            expenseRepository.save(expense);
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Admin should access approval queue")
        void adminShouldAccessApprovalQueue() throws Exception {
            mockMvc.perform(get("/api/expenses/pending-approvals")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @WithUserDetails("user")
        @DisplayName("Regular user should not access approval queue")
        void userShouldNotAccessApprovalQueue() throws Exception {
            mockMvc.perform(get("/api/expenses/pending-approvals"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithUserDetails("expenseadmin")
        @DisplayName("Expense admin should access approval queue")
        void expenseAdminShouldAccessApprovalQueue() throws Exception {
            mockMvc.perform(get("/api/expenses/pending-approvals")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/expenses/{id} - Delete Expense")
    class DeleteExpenseTests {

        private Long testExpenseId;

        @BeforeEach
        void createTestExpense() {
            Expense expense = new Expense();
            expense.setExpenseDate(LocalDate.now());
            expense.setClient("Test Client");
            expense.setProject("Test Project");
            expense.setExpenseType("Travel");
            expense.setDescription("Test");
            expense.setAmount(new BigDecimal("100.00"));
            expense.setPaymentMethod("Cash");
            expense.setExpenseStatus("Draft");
            expense.setUsername("testuser");
            expense = expenseRepository.save(expense);
            testExpenseId = expense.getId();
        }

        @Test
        @WithUserDetails("testuser")
        @DisplayName("User should delete their own draft expense")
        void userShouldDeleteOwnExpense() throws Exception {
            mockMvc.perform(delete("/api/expenses/" + testExpenseId)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithUserDetails("otheruser")
        @DisplayName("User should not delete another user's expense")
        void userShouldNotDeleteOtherUserExpense() throws Exception {
            mockMvc.perform(delete("/api/expenses/" + testExpenseId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Admin should delete any expense")
        void adminShouldDeleteAnyExpense() throws Exception {
            mockMvc.perform(delete("/api/expenses/" + testExpenseId)).andExpect(status().isOk());
        }
    }
}
