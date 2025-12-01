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
import { Expense } from '../../models/expense.model';
import { DropdownValue } from '../../models/task-activity.model';
import { DropdownService } from '../../services/dropdown.service';

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
  isAddMode: boolean = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<ExpenseEditDialogComponent>,
    private readonly dropdownService: DropdownService,
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
  }

  loadDropdowns(): void {
    this.dropdownService.getClients().subscribe({
      next: (data) => (this.clients = data),
      error: (err) => console.error('Error loading clients:', err),
    });

    this.dropdownService.getProjects().subscribe({
      next: (data) => (this.projects = data),
      error: (err) => console.error('Error loading projects:', err),
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
      this.dialogRef.close(updatedExpense);
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
