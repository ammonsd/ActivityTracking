import { Component, Inject } from '@angular/core';
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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { DropdownValue } from '../../models/task-activity.model';

@Component({
  selector: 'app-dropdown-edit-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule,
  ],
  templateUrl: './dropdown-edit-dialog.component.html',
  styleUrl: './dropdown-edit-dialog.component.scss',
})
export class DropdownEditDialogComponent {
  dropdownForm: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<DropdownEditDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: { item: DropdownValue; category: string }
  ) {
    this.dropdownForm = this.fb.group({
      itemValue: [data.item.itemValue, Validators.required],
      displayOrder: [
        data.item.displayOrder,
        [Validators.required, Validators.min(1)],
      ],
      isActive: [data.item.isActive],
    });
  }

  onSubmit(): void {
    if (this.dropdownForm.valid) {
      const updatedItem: DropdownValue = {
        ...this.data.item,
        ...this.dropdownForm.value,
      };
      this.dialogRef.close(updatedItem);
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
