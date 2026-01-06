import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  MatDialogModule,
  MatDialogRef,
  MAT_DIALOG_DATA,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ExpenseService } from '../../services/expense.service';

@Component({
  selector: 'app-receipt-upload-dialog',
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './receipt-upload-dialog.component.html',
  styleUrl: './receipt-upload-dialog.component.scss',
})
export class ReceiptUploadDialogComponent implements OnInit {
  selectedFile: File | null = null;
  uploading = false;
  error: string | null = null;
  maxFileSize = 0;
  maxFileSizeMB = 0;
  maxFileSizeMessage = '';

  constructor(
    private readonly dialogRef: MatDialogRef<ReceiptUploadDialogComponent>,
    private readonly expenseService: ExpenseService,
    @Inject(MAT_DIALOG_DATA)
    public data: { expenseId: number; currentReceiptPath?: string }
  ) {}

  ngOnInit(): void {
    this.loadMaxFileSize();
  }

  loadMaxFileSize(): void {
    this.expenseService.getMaxFileSize().subscribe({
      next: (response) => {
        if (response.data) {
          this.maxFileSize = response.data.maxFileSizeBytes;
          this.maxFileSizeMB = response.data.maxFileSizeMB;
          this.maxFileSizeMessage = response.data.message;
        }
      },
      error: (err) => {
        console.error('Error loading max file size:', err);
        // Use default values if we can't load from server
        this.maxFileSize = 5242880; // 5MB default
        this.maxFileSizeMB = 5;
        this.maxFileSizeMessage = 'Maximum file size: 5MB';
      },
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.error = null;

      // Validate file size
      if (this.maxFileSize > 0 && file.size > this.maxFileSize) {
        this.error = `File size exceeds maximum of ${this.maxFileSizeMB}MB`;
        this.selectedFile = null;
        // Clear the input so user can select the same file again after seeing the error
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
        this.error = 'Invalid file type. Please upload JPG, PNG, or PDF';
        this.selectedFile = null;
        // Clear the input so user can select the same file again after seeing the error
        input.value = '';
        return;
      }

      this.selectedFile = file;
    }
  }

  onUpload(): void {
    if (!this.selectedFile) {
      return;
    }

    this.uploading = true;
    this.error = null;

    this.expenseService
      .uploadReceipt(this.data.expenseId, this.selectedFile)
      .subscribe({
        next: (response) => {
          this.uploading = false;
          this.dialogRef.close({ success: true, receiptPath: response.data });
        },
        error: (err) => {
          this.uploading = false;
          this.error = err.error?.message || 'Failed to upload receipt';
          console.error('Error uploading receipt:', err);
        },
      });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }
}
