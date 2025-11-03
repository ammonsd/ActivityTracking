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
import { TaskActivity, DropdownValue } from '../../models/task-activity.model';
import { DropdownService } from '../../services/dropdown.service';

@Component({
  selector: 'app-task-edit-dialog',
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
  ],
  templateUrl: './task-edit-dialog.component.html',
  styleUrl: './task-edit-dialog.component.scss',
})
export class TaskEditDialogComponent implements OnInit {
  taskForm: FormGroup;
  clients: DropdownValue[] = [];
  projects: DropdownValue[] = [];
  phases: DropdownValue[] = [];

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<TaskEditDialogComponent>,
    private readonly dropdownService: DropdownService,
    @Inject(MAT_DIALOG_DATA) public data: { task: TaskActivity }
  ) {
    this.taskForm = this.fb.group({
      taskDate: [data.task.taskDate, Validators.required],
      client: [data.task.client, Validators.required],
      project: [data.task.project, Validators.required],
      phase: [data.task.phase, Validators.required],
      hours: [
        data.task.hours,
        [Validators.required, Validators.min(0.25), Validators.max(24)],
      ],
      details: [data.task.details, Validators.required],
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

    this.dropdownService.getPhases().subscribe({
      next: (data) => (this.phases = data),
      error: (err) => console.error('Error loading phases:', err),
    });
  }

  onSubmit(): void {
    if (this.taskForm.valid) {
      const updatedTask: TaskActivity = {
        ...this.data.task,
        ...this.taskForm.value,
      };
      this.dialogRef.close(updatedTask);
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
