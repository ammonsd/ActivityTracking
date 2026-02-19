/**
 * Description: CSV Export Dialog component - displays exported CSV data in a dialog window
 * with copy to clipboard and file download capabilities, matching the Spring Boot CSV export UI.
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  MatDialogModule,
  MAT_DIALOG_DATA,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

export interface CsvExportDialogData {
  title: string;
  csvData: string;
  filename: string;
}

@Component({
  selector: 'app-csv-export-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
  ],
  template: `
    <div class="csv-export-dialog">
      <h2 mat-dialog-title>{{ data.title }}</h2>
      <mat-dialog-content>
        <p class="instruction-text">
          Copy the CSV data below (filtered entries only):
        </p>
        <textarea
          class="csv-textarea"
          readonly
          [value]="data.csvData"
          rows="15"
        ></textarea>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-raised-button color="primary" (click)="copyToClipboard()">
          <mat-icon>content_copy</mat-icon> Copy to Clipboard
        </button>
        <button mat-raised-button color="accent" (click)="downloadCSV()">
          <mat-icon>file_download</mat-icon> Download CSV
        </button>
        <button mat-button (click)="close()">Close</button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .csv-export-dialog {
        min-width: 600px;
        max-width: 900px;
      }

      h2[mat-dialog-title] {
        color: #1565c0;
        margin: 0;
        padding: 20px 24px 8px;
        font-size: 1.25rem;
        font-weight: 500;
      }

      .instruction-text {
        margin: 0 0 12px 0;
        color: #555;
        font-size: 0.9rem;
      }

      .csv-textarea {
        width: 100%;
        font-family: 'Courier New', monospace;
        font-size: 0.8rem;
        line-height: 1.5;
        padding: 10px;
        border: 1px solid #ccc;
        border-radius: 4px;
        background: #f9f9f9;
        resize: vertical;
        box-sizing: border-box;
        color: #333;
      }

      mat-dialog-content {
        padding: 8px 24px 16px;
      }

      mat-dialog-actions {
        padding: 8px 24px 16px;
        gap: 8px;
      }
    `,
  ],
})
export class CsvExportDialogComponent {
  constructor(
    @Inject(MAT_DIALOG_DATA) public readonly data: CsvExportDialogData,
    private readonly dialogRef: MatDialogRef<CsvExportDialogComponent>,
    private readonly snackBar: MatSnackBar,
  ) {}

  /**
   * Copies the CSV content to the system clipboard and shows a confirmation snack bar.
   */
  copyToClipboard(): void {
    navigator.clipboard
      .writeText(this.data.csvData)
      .then(() => {
        this.snackBar.open('CSV copied to clipboard!', 'Close', {
          duration: 3000,
        });
      })
      .catch(() => {
        this.snackBar.open('Failed to copy to clipboard.', 'Close', {
          duration: 3000,
        });
      });
  }

  /**
   * Triggers a browser file download of the CSV data using a temporary anchor element.
   */
  downloadCSV(): void {
    const blob = new Blob([this.data.csvData], {
      type: 'text/csv;charset=utf-8;',
    });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = this.data.filename;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  close(): void {
    this.dialogRef.close();
  }
}
