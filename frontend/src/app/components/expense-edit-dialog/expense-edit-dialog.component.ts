/**
 * Description: Expense Edit Dialog component - provides a dialog for creating and editing expenses with receipt upload
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import {
  MatDialogModule,
  MatDialogRef,
  MAT_DIALOG_DATA,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Expense } from '../../models/expense.model';
import { DropdownValue } from '../../models/task-activity.model';
import { DropdownService } from '../../services/dropdown.service';
import { ExpenseService } from '../../services/expense.service';

@Component({
  selector: 'app-expense-edit-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './expense-edit-dialog.component.html',
  styleUrl: './expense-edit-dialog.component.scss',
})
export class ExpenseEditDialogComponent implements OnInit {
  expenseForm: FormGroup;
  clients: DropdownValue[] = [];
  projects: DropdownValue[] = [];
  expenseTypes: DropdownValue[] = [];
  paymentMethods: DropdownValue[] = [];
  currencies: DropdownValue[] = [];
  vendors: DropdownValue[] = [];
  isAddMode: boolean = false;
  
  // Receipt upload properties
  selectedFile: File | null = null;
  maxFileSize = 5242880; // 5MB default
  maxFileSizeMB = 5;
  fileError: string | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<ExpenseEditDialogComponent>,
    private readonly dropdownService: DropdownService,
    private readonly expenseService: ExpenseService,
    @Inject(MAT_DIALOG_DATA)
    public data: { expense: Expense; isAddMode?: boolean }
  ) {
    this.isAddMode = data.isAddMode || false;

    // Convert string date to Date object for the datepicker
    let expenseDate = null;
    if (data.expense.expenseDate) {
      const parts = data.expense.expenseDate.split('-');
      expenseDate = new Date(
        Number.parseInt(parts[0]),
        Number.parseInt(parts[1]) - 1,
        Number.parseInt(parts[2])
      );
    }

    this.expenseForm = this.fb.group({
      expenseDate: [expenseDate, Validators.required],
      client: [data.expense.client, Validators.required],
      project: [data.expense.project || ''],
      expenseType: [data.expense.expenseType, Validators.required],
      description: [data.expense.description, Validators.required],
      amount: [
        data.expense.amount,
        [Validators.required, Validators.min(0.01)],
      ],
      currency: [data.expense.currency || 'USD', Validators.required],
      paymentMethod: [data.expense.paymentMethod, Validators.required],
      vendor: [data.expense.vendor || ''],
      referenceNumber: [data.expense.referenceNumber || ''],
      notes: [data.expense.notes || ''],
    });
  }

  ngOnInit(): void {
    this.loadDropdowns();
    this.loadMaxFileSize();
  }

  loadMaxFileSize(): void {
    this.expenseService.getMaxFileSize().subscribe({
      next: (response) => {
        if (response.data) {
          this.maxFileSize = response.data.maxFileSizeBytes;
          this.maxFileSizeMB = response.data.maxFileSizeMB;
        }
      },
      error: (err) => {
        console.error('Error loading max file size:', err);
        // Use default values if we can't load from server
        this.maxFileSize = 5242880; // 5MB default
        this.maxFileSizeMB = 5;
      },
    });
  }

  loadDropdowns(): void {
    this.dropdownService.getExpenseClients().subscribe({
      next: (data) => (this.clients = data),
      error: (err) => console.error('Error loading expense clients:', err),
    });

    this.dropdownService.getExpenseProjects().subscribe({
      next: (data) => (this.projects = data),
      error: (err) => console.error('Error loading expense projects:', err),
    });

    this.dropdownService.getExpenseTypes().subscribe({
      next: (data) => (this.expenseTypes = data),
      error: (err) => console.error('Error loading expense types:', err),
    });

    this.dropdownService.getPaymentMethods().subscribe({
      next: (data) => (this.paymentMethods = data),
      error: (err) => console.error('Error loading payment methods:', err),
    });

    this.dropdownService.getCurrencies().subscribe({
      next: (data) => (this.currencies = data),
      error: (err) => console.error('Error loading currencies:', err),
    });

    this.dropdownService.getVendors().subscribe({
      next: (data) => (this.vendors = data),
      error: (err) => console.error('Error loading vendors:', err),
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.fileError = null;

      // Validate file size
      if (this.maxFileSize > 0 && file.size > this.maxFileSize) {
        this.fileError = `File size exceeds maximum of ${this.maxFileSizeMB}MB`;
        this.selectedFile = null;
        input.value = '';
        return;
      }

      // Validate file type
      const allowedTypes = [
        'image/jpeg',
        'image/jpg',
        'image/png',
        'application/pdf',
      ];
      if (!allowedTypes.includes(file.type)) {
        this.fileError = 'Invalid file type. Please upload JPG, PNG, or PDF';
        this.selectedFile = null;
        input.value = '';
        return;
      }

      this.selectedFile = file;
    }
  }

  removeSelectedFile(): void {
    this.selectedFile = null;
    this.fileError = null;
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }

  onSubmit(): void {
    if (this.expenseForm.valid) {
      // Convert Date object to YYYY-MM-DD string format for the API
      const formValue = this.expenseForm.value;
      const expenseDate = formValue.expenseDate;
      const formattedDate =
        expenseDate instanceof Date
          ? expenseDate.toISOString().split('T')[0]
          : expenseDate;

      const updatedExpense: Expense = {
        ...this.data.expense,
        ...formValue,
        expenseDate: formattedDate,
        expenseStatus: this.data.expense.expenseStatus || 'Draft',
      };
      
      // Return both the expense data and the selected file
      this.dialogRef.close({ 
        expense: updatedExpense, 
        receiptFile: this.selectedFile 
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
