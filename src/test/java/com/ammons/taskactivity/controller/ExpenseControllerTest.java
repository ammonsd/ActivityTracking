package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.config.TestSecurityConfig;
import com.ammons.taskactivity.dto.ExpenseDto;
import com.ammons.taskactivity.entity.Expense;
import com.ammons.taskactivity.repository.ExpenseRepository;
import com.ammons.taskactivity.repository.UserRepository;
import com.ammons.taskactivity.security.JwtAuthenticationFilter;
import com.ammons.taskactivity.security.JwtUtil;
import com.ammons.taskactivity.service.ExpenseService;
import com.ammons.taskactivity.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ExpenseControllerTest - Unit tests for ExpenseController REST API Tests all endpoints with proper
 * authentication, validation, authorization, and error handling
 *
 * @author Dean Ammons
 * @version 1.0
 */
@WebMvcTest(ExpenseController.class)
@Import(TestSecurityConfig.class)
@DisplayName("ExpenseController Tests")
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExpenseService expenseService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ExpenseRepository expenseRepository;

    @MockitoBean
    private com.ammons.taskactivity.security.JwtUtil jwtUtil;

    @MockitoBean
    private com.ammons.taskactivity.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    private Expense testExpense;
    private ExpenseDto testExpenseDto;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2025, 11, 15);

        testExpense = new Expense();
        testExpense.setId(1L);
        testExpense.setUsername("testuser");
        testExpense.setClient("Test Client");
        testExpense.setProject("Test Project");
        testExpense.setExpenseDate(testDate);
        testExpense.setExpenseType("Travel - Airfare");
        testExpense.setDescription("Flight to client site");
        testExpense.setAmount(new BigDecimal("500.00"));
        testExpense.setCurrency("USD");
        testExpense.setPaymentMethod("Corporate Credit Card");
        testExpense.setExpenseStatus("Draft");

        testExpenseDto = new ExpenseDto();
        testExpenseDto.setClient("Test Client");
        testExpenseDto.setProject("Test Project");
        testExpenseDto.setExpenseDate(testDate);
        testExpenseDto.setExpenseType("Travel - Airfare");
        testExpenseDto.setDescription("Flight to client site");
        testExpenseDto.setAmount(new BigDecimal("500.00"));
        testExpenseDto.setCurrency("USD");
        testExpenseDto.setPaymentMethod("Corporate Credit Card");
        testExpenseDto.setExpenseStatus("Draft");
    }

    @Nested
    @DisplayName("Create Expense Tests")
    class CreateExpenseTests {

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should create expense successfully")
        void shouldCreateExpenseSuccessfully() throws Exception {
            // Given
            when(expenseService.createExpense(any(ExpenseDto.class))).thenReturn(testExpense);

            // When/Then
            mockMvc.perform(
                    post("/api/expenses").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testExpenseDto)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Expense created successfully"))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.client").value("Test Client"))
                    .andExpect(jsonPath("$.data.amount").value(500.00));

            verify(expenseService, times(1)).createExpense(any(ExpenseDto.class));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(
                    post("/api/expenses").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testExpenseDto)))
                    .andExpect(status().isUnauthorized());

            verify(expenseService, never()).createExpense(any(ExpenseDto.class));
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should return validation errors for invalid data")
        void shouldReturnValidationErrorsForInvalidData() throws Exception {
            // Given
            ExpenseDto invalidDto = new ExpenseDto();
            // Missing required fields

            // When/Then
            mockMvc.perform(
                    post("/api/expenses").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest());

            verify(expenseService, never()).createExpense(any(ExpenseDto.class));
        }
    }

    @Nested
    @DisplayName("Get Expenses Tests")
    class GetExpensesTests {

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should get all expenses for user")
        void shouldGetAllExpensesForUser() throws Exception {
            // Given
            Page<Expense> page = new PageImpl<>(Arrays.asList(testExpense));
            when(expenseService.getExpensesByFilters(any(), any())).thenReturn(page);

            // When/Then
            mockMvc.perform(get("/api/expenses").param("page", "0").param("size", "20"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].id").value(1));

            verify(expenseService, times(1)).getExpensesByFilters(any(), any());
        }

        @Test
        @WithMockUser(username = "testuser", roles = "ADMIN")
        @DisplayName("Should get all expenses for admin")
        void shouldGetAllExpensesForAdmin() throws Exception {
            // Given
            Page<Expense> page = new PageImpl<>(Arrays.asList(testExpense));
            when(expenseService.getExpensesByFilters(any(), any())).thenReturn(page);

            // When/Then
            mockMvc.perform(get("/api/expenses").param("page", "0").param("size", "20"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

            verify(expenseService, times(1)).getExpensesByFilters(any(), any());
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should get expense by ID when user owns it")
        void shouldGetExpenseByIdWhenUserOwnsIt() throws Exception {
            // Given
            when(expenseService.getExpenseById(1L)).thenReturn(Optional.of(testExpense));

            // When/Then
            mockMvc.perform(get("/api/expenses/1")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.username").value("testuser"));

            verify(expenseService, times(1)).getExpenseById(1L);
        }

        @Test
        @WithMockUser(username = "otheruser", roles = "USER")
        @DisplayName("Should return 403 when user tries to access other user's expense")
        void shouldReturn403WhenUserTriesToAccessOtherUsersExpense() throws Exception {
            // Given
            when(expenseService.getExpenseById(1L)).thenReturn(Optional.of(testExpense));

            // When/Then
            mockMvc.perform(get("/api/expenses/1")).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("your own expenses")));

            verify(expenseService, times(1)).getExpenseById(1L);
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        @DisplayName("Should allow admin to access any expense")
        void shouldAllowAdminToAccessAnyExpense() throws Exception {
            // Given
            when(expenseService.getExpenseById(1L)).thenReturn(Optional.of(testExpense));

            // When/Then
            mockMvc.perform(get("/api/expenses/1")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(expenseService, times(1)).getExpenseById(1L);
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should return 404 when expense not found")
        void shouldReturn404WhenExpenseNotFound() throws Exception {
            // Given
            when(expenseService.getExpenseById(999L)).thenReturn(Optional.empty());

            // When/Then
            mockMvc.perform(get("/api/expenses/999")).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Expense not found"));

            verify(expenseService, times(1)).getExpenseById(999L);
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should get current week expenses")
        void shouldGetCurrentWeekExpenses() throws Exception {
            // Given
            List<Expense> expenses = Arrays.asList(testExpense);
            when(expenseService.getExpensesInDateRangeForUser(anyString(), any(LocalDate.class),
                    any(LocalDate.class))).thenReturn(expenses);

            // When/Then
            mockMvc.perform(get("/api/expenses/current-week")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)));

            verify(expenseService, times(1)).getExpensesInDateRangeForUser(anyString(),
                    any(LocalDate.class), any(LocalDate.class));
        }
    }

    @Nested
    @DisplayName("Update Expense Tests")
    class UpdateExpenseTests {

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should update expense when user owns it")
        void shouldUpdateExpenseWhenUserOwnsIt() throws Exception {
            // Given
            when(expenseService.getExpenseById(1L)).thenReturn(Optional.of(testExpense));
            when(expenseService.updateExpense(eq(1L), any(ExpenseDto.class)))
                    .thenReturn(testExpense);

            // When/Then
            mockMvc.perform(
                    put("/api/expenses/1").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testExpenseDto)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Expense updated successfully"));

            verify(expenseService, times(1)).getExpenseById(1L);
            verify(expenseService, times(1)).updateExpense(eq(1L), any(ExpenseDto.class));
        }

        @Test
        @WithMockUser(username = "otheruser", roles = "USER")
        @DisplayName("Should return 403 when user tries to update other user's expense")
        void shouldReturn403WhenUserTriesToUpdateOtherUsersExpense() throws Exception {
            // Given
            when(expenseService.getExpenseById(1L)).thenReturn(Optional.of(testExpense));

            // When/Then
            mockMvc.perform(
                    put("/api/expenses/1").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testExpenseDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));

            verify(expenseService, times(1)).getExpenseById(1L);
            verify(expenseService, never()).updateExpense(anyLong(), any(ExpenseDto.class));
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should return 403 when user tries to modify protected fields")
        void shouldReturn403WhenUserTriesToModifyProtectedFields() throws Exception {
            // Given
            when(expenseService.getExpenseById(1L)).thenReturn(Optional.of(testExpense));
            testExpenseDto.setApprovedBy("someuser");

            // When/Then
            mockMvc.perform(
                    put("/api/expenses/1").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testExpenseDto)))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.message")
                            .value(containsString("approval or reimbursement")));

            verify(expenseService, times(1)).getExpenseById(1L);
            verify(expenseService, never()).updateExpense(anyLong(), any(ExpenseDto.class));
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        @DisplayName("Should allow admin to update any expense")
        void shouldAllowAdminToUpdateAnyExpense() throws Exception {
            // Given
            when(expenseService.getExpenseById(1L)).thenReturn(Optional.of(testExpense));
            when(expenseService.updateExpense(eq(1L), any(ExpenseDto.class)))
                    .thenReturn(testExpense);

            // When/Then
            mockMvc.perform(
                    put("/api/expenses/1").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testExpenseDto)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

            verify(expenseService, times(1)).updateExpense(eq(1L), any(ExpenseDto.class));
        }
    }

    @Nested
    @DisplayName("Delete Expense Tests")
    class DeleteExpenseTests {

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should delete expense when user owns it")
        void shouldDeleteExpenseWhenUserOwnsIt() throws Exception {
            // Given
            when(expenseService.getExpenseById(1L)).thenReturn(Optional.of(testExpense));
            doNothing().when(expenseService).deleteExpense(1L);

            // When/Then
            mockMvc.perform(delete("/api/expenses/1").with(csrf())).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Expense deleted successfully"));

            verify(expenseService, times(1)).deleteExpense(1L);
        }

        @Test
        @WithMockUser(username = "otheruser", roles = "USER")
        @DisplayName("Should return 403 when user tries to delete other user's expense")
        void shouldReturn403WhenUserTriesToDeleteOtherUsersExpense() throws Exception {
            // Given
            when(expenseService.getExpenseById(1L)).thenReturn(Optional.of(testExpense));

            // When/Then
            mockMvc.perform(delete("/api/expenses/1").with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));

            verify(expenseService, never()).deleteExpense(anyLong());
        }
    }

    @Nested
    @DisplayName("Status Management Tests")
    class StatusManagementTests {

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should submit expense for approval")
        void shouldSubmitExpenseForApproval() throws Exception {
            // Given
            when(expenseService.submitForApproval(1L, "testuser")).thenReturn(testExpense);

            // When/Then
            mockMvc.perform(post("/api/expenses/1/submit").with(csrf())).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Expense submitted for approval"));

            verify(expenseService, times(1)).submitForApproval(1L, "testuser");
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should return 400 when expense cannot be submitted")
        void shouldReturn400WhenExpenseCannotBeSubmitted() throws Exception {
            // Given
            when(expenseService.submitForApproval(1L, "testuser"))
                    .thenThrow(new IllegalStateException("Cannot submit"));

            // When/Then
            mockMvc.perform(post("/api/expenses/1/submit").with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(expenseService, times(1)).submitForApproval(1L, "testuser");
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        @DisplayName("Should approve expense")
        void shouldApproveExpense() throws Exception {
            // Given
            when(expenseService.approveExpense(1L, "admin", "Approved")).thenReturn(testExpense);

            // When/Then
            mockMvc.perform(post("/api/expenses/1/approve").with(csrf()).param("notes", "Approved"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Expense approved"));

            verify(expenseService, times(1)).approveExpense(1L, "admin", "Approved");
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        @DisplayName("Should reject expense")
        void shouldRejectExpense() throws Exception {
            // Given
            when(expenseService.rejectExpense(1L, "admin", "Needs more info"))
                    .thenReturn(testExpense);

            // When/Then
            mockMvc.perform(
                    post("/api/expenses/1/reject").with(csrf()).param("notes", "Needs more info"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Expense rejected"));

            verify(expenseService, times(1)).rejectExpense(1L, "admin", "Needs more info");
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        @DisplayName("Should mark expense as reimbursed")
        void shouldMarkExpenseAsReimbursed() throws Exception {
            // Given
            when(expenseService.markAsReimbursed(1L, "admin", new BigDecimal("100.00"),
                            "Reimbursed"))
                    .thenReturn(testExpense);

            // When/Then
            mockMvc.perform(
                            post("/api/expenses/1/reimburse").with(csrf())
                                            .param("reimbursedAmount", "100.00")
                                            .param("notes", "Reimbursed"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Expense marked as reimbursed"));

            verify(expenseService, times(1)).markAsReimbursed(1L, "admin", new BigDecimal("100.00"),
                            "Reimbursed");
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        @DisplayName("Should get pending approvals")
        void shouldGetPendingApprovals() throws Exception {
            // Given
            List<Expense> expenses = Arrays.asList(testExpense);
            when(expenseService.getPendingApprovals()).thenReturn(expenses);

            // When/Then
            mockMvc.perform(get("/api/expenses/pending-approvals")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)));

            verify(expenseService, times(1)).getPendingApprovals();
        }
    }

    @Nested
    @DisplayName("Aggregate Tests")
    class AggregateTests {

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should get total expenses")
        void shouldGetTotalExpenses() throws Exception {
            // Given
            when(expenseService.getTotalAmountByUserAndDateRange(anyString(), any(LocalDate.class),
                    any(LocalDate.class))).thenReturn(1500.00);

            // When/Then
            mockMvc.perform(get("/api/expenses/total").param("startDate", "2025-11-01")
                    .param("endDate", "2025-11-30")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(1500.00));

            verify(expenseService, times(1)).getTotalAmountByUserAndDateRange(anyString(),
                    any(LocalDate.class), any(LocalDate.class));
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should get total by status")
        void shouldGetTotalByStatus() throws Exception {
            // Given
            when(expenseService.getTotalAmountByUserAndStatus("testuser", "Draft"))
                    .thenReturn(500.00);

            // When/Then
            mockMvc.perform(get("/api/expenses/total-by-status").param("status", "Draft"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(500.00));

            verify(expenseService, times(1)).getTotalAmountByUserAndStatus("testuser", "Draft");
        }
    }
}
