/**
 * Description: Expense List component - displays and manages expenses with filtering and receipt upload capabilities
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ExpenseService } from '../../services/expense.service';
import { AuthService } from '../../services/auth.service';
import { Expense } from '../../models/expense.model';
import { ExpenseEditDialogComponent } from '../expense-edit-dialog/expense-edit-dialog.component';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { ReceiptUploadDialogComponent } from '../receipt-upload-dialog/receipt-upload-dialog.component';

@Component({
  selector: 'app-expense-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSnackBarModule,
    MatTooltipModule,
  ],
  templateUrl: './expense-list.component.html',
  styleUrl: './expense-list.component.scss',
})
export class ExpenseListComponent implements OnInit {
  expenses: Expense[] = [];
  filteredExpenses: Expense[] = [];
  displayedColumns: string[] = [];
  loading = false;
  error: string | null = null;
  currentUser = '';
  currentRole = '';
  currentDate = new Date();
  canAccessExpenses = true;

  // Pagination properties
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;

  // Filter properties
  selectedClient = '';
  selectedProject = '';
  selectedExpenseType = '';
  selectedStatus = '';
  selectedPaymentMethod = '';
  selectedUsername = '';
  startDate: Date | null = null;
  endDate: Date | null = null;
  uniqueClients: string[] = [];
  uniqueProjects: string[] = [];
  uniqueExpenseTypes: string[] = [];
  uniqueStatuses: string[] = [];
  uniquePaymentMethods: string[] = [];
  uniqueUsernames: string[] = [];

  // Computed properties for pagination display
  get startEntry(): number {
    return this.totalElements === 0 ? 0 : this.currentPage * this.pageSize + 1;
  }

  get endEntry(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
  }

  constructor(
    private readonly expenseService: ExpenseService,
    private readonly authService: AuthService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUsername();
    this.currentRole = this.authService.getCurrentRole();

    // Check if user has email for expense access
    this.expenseService.canAccessExpenses().subscribe({
      next: (response) => {
        this.canAccessExpenses = response.data || false;
        if (this.canAccessExpenses) {
          this.loadExpenses();
        }
      },
      error: (err) => {
        console.error('Error checking expense access:', err);
        this.canAccessExpenses = false;
      },
    });

    // Set displayed columns based on role
    // ADMIN and EXPENSE_ADMIN see username column, others don't
    if (this.currentRole === 'ADMIN' || this.currentRole === 'EXPENSE_ADMIN') {
      this.displayedColumns = [
        'expenseDate',
        'client',
        'project',
        'expenseType',
        'description',
        'amount',
        'expenseStatus',
        'username',
        'receipt',
      ];
    } else {
      this.displayedColumns = [
        'expenseDate',
        'client',
        'project',
        'expenseType',
        'description',
        'amount',
        'expenseStatus',
        'receipt',
      ];
    }
  }

  loadExpenses(): void {
    this.loading = true;
    this.error = null;

    // Format dates for API (without timezone conversion)
    const formatDate = (date: Date | null): string | undefined => {
      if (!date) return undefined;
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    };

    const startDateStr = formatDate(this.startDate);
    const endDateStr = formatDate(this.endDate);

    // Only pass filter values if they're not empty strings
    const clientFilter = this.selectedClient ? this.selectedClient : undefined;
    const projectFilter = this.selectedProject
      ? this.selectedProject
      : undefined;
    const expenseTypeFilter = this.selectedExpenseType
      ? this.selectedExpenseType
      : undefined;
    const statusFilter = this.selectedStatus ? this.selectedStatus : undefined;
    const paymentMethodFilter = this.selectedPaymentMethod
      ? this.selectedPaymentMethod
      : undefined;
    const usernameFilter = this.selectedUsername
      ? this.selectedUsername
      : undefined;

    this.expenseService
      .getAllExpenses(
        this.currentPage,
        this.pageSize,
        clientFilter,
        projectFilter,
        expenseTypeFilter,
        statusFilter,
        paymentMethodFilter,
        startDateStr,
        endDateStr,
        usernameFilter
      )
      .subscribe({
        next: (response) => {
          // Handle ApiResponse wrapper with Page object
          // response.data is a Page object with content, totalElements, etc.
          const pageData = response.data as any;
          this.expenses = pageData?.content || [];
          this.filteredExpenses = this.expenses;

          // Extract pagination metadata from Page object
          this.totalElements = pageData?.totalElements || 0;
          this.totalPages = pageData?.totalPages || 0;
          this.currentPage = pageData?.number || 0;

          // Extract unique values for filters from current page
          const currentClients = [
            ...new Set(this.expenses.map((e) => e.client)),
          ];
          const currentProjects = [
            ...new Set(
              this.expenses.map((e) => e.project).filter((p) => p !== null)
            ),
          ] as string[];
          const currentExpenseTypes = [
            ...new Set(this.expenses.map((e) => e.expenseType)),
          ];
          const currentStatuses = [
            ...new Set(this.expenses.map((e) => e.expenseStatus)),
          ];
          const currentPaymentMethods = [
            ...new Set(this.expenses.map((e) => e.paymentMethod)),
          ];
          const currentUsernames = [
            ...new Set(
              this.expenses.map((e) => e.username).filter((u) => u !== null)
            ),
          ] as string[];

          // Merge with existing unique values
          this.uniqueClients = [
            ...new Set([...this.uniqueClients, ...currentClients]),
          ].sort((a, b) => a.localeCompare(b));
          this.uniqueProjects = [
            ...new Set([...this.uniqueProjects, ...currentProjects]),
          ].sort((a, b) => a.localeCompare(b));
          this.uniqueExpenseTypes = [
            ...new Set([...this.uniqueExpenseTypes, ...currentExpenseTypes]),
          ].sort((a, b) => a.localeCompare(b));
          this.uniqueStatuses = [
            ...new Set([...this.uniqueStatuses, ...currentStatuses]),
          ].sort((a, b) => a.localeCompare(b));
          this.uniquePaymentMethods = [
            ...new Set([
              ...this.uniquePaymentMethods,
              ...currentPaymentMethods,
            ]),
          ].sort((a, b) => a.localeCompare(b));
          this.uniqueUsernames = [
            ...new Set([...this.uniqueUsernames, ...currentUsernames]),
          ].sort((a, b) => a.localeCompare(b));

          this.loading = false;
          console.log('Loaded expenses:', this.expenses);
        },
        error: (err) => {
          console.error('Error loading expenses:', err);
          // Try to extract error message from backend
          if (err.error?.message) {
            this.error = err.error.message;
          } else if (err.message) {
            this.error = err.message;
          } else {
            this.error =
              'Failed to load expenses. Make sure the Spring Boot backend is running.';
          }
          this.loading = false;
        },
      });
  }

  // Pagination methods
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadExpenses();
    }
  }

  applyFilters(): void {
    // Reset to first page when filters change and reload from server
    this.currentPage = 0;
    this.loadExpenses();
  }

  clearFilters(): void {
    this.selectedClient = '';
    this.selectedProject = '';
    this.selectedExpenseType = '';
    this.selectedStatus = '';
    this.selectedPaymentMethod = '';
    this.selectedUsername = '';
    this.startDate = null;
    this.endDate = null;
    this.applyFilters();
  }

  addExpense(): void {
    // Create an empty expense with today's date
    const today = new Date().toISOString().split('T')[0];
    const emptyExpense: Expense = {
      expenseDate: today,
      client: '',
      project: '',
      expenseType: '',
      description: '',
      amount: 0,
      currency: 'USD',
      paymentMethod: '',
      vendor: '',
      referenceNumber: '',
      notes: '',
      expenseStatus: 'Draft',
      username: this.authService.getCurrentUsername() || '',
    };

    const dialogRef = this.dialog.open(ExpenseEditDialogComponent, {
      width: '600px',
      maxHeight: '90vh',
      data: { expense: emptyExpense, isAddMode: true },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        console.log('Submitting new expense:', result);
        // Remove the id field if present since this is a new expense
        const { id, ...expenseData } = result;
        this.expenseService.createExpense(expenseData).subscribe({
          next: (response) => {
            console.log('Expense created successfully:', response);
            this.loadExpenses(); // Reload the list
          },
          error: (err) => {
            console.error('Error creating expense:', err);
            console.error('Error status:', err.status);
            console.error('Error message:', err.error);

            let errorMessage = 'Failed to create expense. ';
            if (err.status === 403) {
              errorMessage += 'You do not have permission to create expenses.';
            } else if (err.error?.message) {
              errorMessage += err.error.message;
            } else {
              errorMessage += 'Please try again.';
            }

            alert(errorMessage);
          },
        });
      }
    });
  }

  cloneExpense(expense: Expense): void {
    // Create a copy of the expense with today's date and no ID
    const today = new Date().toISOString().split('T')[0];
    const clonedExpense: Expense = {
      expenseDate: today,
      client: expense.client,
      project: expense.project,
      expenseType: expense.expenseType,
      description: expense.description,
      amount: expense.amount,
      currency: expense.currency,
      paymentMethod: expense.paymentMethod,
      vendor: expense.vendor,
      referenceNumber: expense.referenceNumber,
      notes: expense.notes,
      expenseStatus: 'Draft',
      username: this.authService.getCurrentUsername() || '',
    };

    const dialogRef = this.dialog.open(ExpenseEditDialogComponent, {
      width: '600px',
      maxHeight: '90vh',
      data: { expense: clonedExpense, isAddMode: true },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        console.log('Submitting cloned expense:', result);
        // Remove the id field if present since this is a new expense
        const { id, ...expenseData } = result;
        this.expenseService.createExpense(expenseData).subscribe({
          next: (response) => {
            console.log('Expense cloned successfully:', response);
            this.loadExpenses(); // Reload the list
          },
          error: (err) => {
            console.error('Error cloning expense:', err);
            console.error('Error status:', err.status);
            console.error('Error message:', err.error);

            let errorMessage = 'Failed to clone expense. ';
            if (err.status === 403) {
              errorMessage += 'You do not have permission to create expenses.';
            } else if (err.error?.message) {
              errorMessage += err.error.message;
            } else {
              errorMessage += 'Please try again.';
            }

            alert(errorMessage);
          },
        });
      }
    });
  }

  editExpense(expense: Expense): void {
    const dialogRef = this.dialog.open(ExpenseEditDialogComponent, {
      width: '600px',
      maxHeight: '90vh',
      data: { expense: { ...expense }, isAddMode: false }, // Pass a copy
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        console.log('Submitting expense update:', result);
        this.expenseService.updateExpense(result.id, result).subscribe({
          next: (response) => {
            console.log('Expense updated successfully:', response);
            this.loadExpenses(); // Reload the list
          },
          error: (err) => {
            console.error('Error updating expense:', err);
            console.error('Error status:', err.status);
            console.error('Error message:', err.error);

            let errorMessage = 'Failed to update expense. ';
            if (err.status === 403) {
              errorMessage +=
                'You do not have permission to update this expense.';
            } else if (err.status === 404) {
              errorMessage += 'The expense no longer exists.';
            } else if (err.error?.message) {
              errorMessage += err.error.message;
            } else {
              errorMessage += 'Please try again.';
            }

            alert(errorMessage);
          },
        });
      }
    });
  }

  deleteExpense(expense: Expense): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Delete',
        message: `Are you sure you want to delete this expense from ${expense.expenseDate}?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.expenseService.deleteExpense(expense.id!).subscribe({
          next: () => {
            console.log('Expense deleted successfully');
            this.loadExpenses(); // Reload the list
          },
          error: (err) => {
            console.error('Error deleting expense:', err);
            alert('Failed to delete expense. You may not have permission.');
          },
        });
      }
    });
  }

  formatCurrency(amount: number, currency: string): string {
    if (currency === 'USD' || !currency) {
      return `$${amount.toFixed(2)}`;
    }
    return `${currency} ${amount.toFixed(2)}`;
  }

  // Receipt Management Methods

  uploadReceipt(expense: Expense): void {
    const dialogRef = this.dialog.open(ReceiptUploadDialogComponent, {
      width: '500px',
      data: {
        expenseId: expense.id,
        currentReceiptPath: expense.receiptPath,
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result?.success) {
        this.snackBar.open('Receipt uploaded successfully', 'Close', {
          duration: 3000,
        });
        this.loadExpenses(); // Reload to show updated receipt status
      }
    });
  }

  downloadReceipt(expense: Expense): void {
    if (!expense.id || !expense.receiptPath) {
      this.snackBar.open('No receipt available', 'Close', { duration: 3000 });
      return;
    }

    this.expenseService.downloadReceipt(expense.id).subscribe({
      next: (blob) => {
        // Create a download link
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        // Extract filename from receiptPath or use default
        const filename = expense.receiptPath?.split('/').pop() || 'receipt';
        link.download = filename;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error downloading receipt:', err);
        this.snackBar.open('Failed to download receipt', 'Close', {
          duration: 3000,
        });
      },
    });
  }

  deleteReceiptConfirm(expense: Expense): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Delete Receipt',
        message: 'Are you sure you want to delete this receipt?',
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result && expense.id) {
        this.expenseService.deleteReceipt(expense.id).subscribe({
          next: () => {
            this.snackBar.open('Receipt deleted successfully', 'Close', {
              duration: 3000,
            });
            this.loadExpenses(); // Reload to show updated status
          },
          error: (err) => {
            console.error('Error deleting receipt:', err);
            this.snackBar.open('Failed to delete receipt', 'Close', {
              duration: 3000,
            });
          },
        });
      }
    });
  }
}
