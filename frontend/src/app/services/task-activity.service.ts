/**
 * Description: Task Activity service - provides HTTP client methods for managing task activities
 *
 * Author: Dean Ammons
 * Date: November 2025
 */

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { TaskActivity, ApiResponse } from '../models/task-activity.model';

@Injectable({
  providedIn: 'root',
})
export class TaskActivityService {
  private apiUrl = `${environment.apiUrl}/task-activities`;

  constructor(private http: HttpClient) {}

  getAllTasks(
    page: number = 0,
    size: number = 20,
    client?: string,
    project?: string,
    phase?: string,
    taskId?: string,
    startDate?: string,
    endDate?: string,
  ): Observable<ApiResponse<TaskActivity[]>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (client) params = params.set('client', client);
    if (project) params = params.set('project', project);
    if (phase) params = params.set('phase', phase);
    if (taskId) params = params.set('taskId', taskId);
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);

    return this.http.get<ApiResponse<TaskActivity[]>>(this.apiUrl, { params });
  }

  getTaskById(id: number): Observable<ApiResponse<TaskActivity>> {
    return this.http.get<ApiResponse<TaskActivity>>(`${this.apiUrl}/${id}`);
  }

  createTask(task: TaskActivity): Observable<ApiResponse<TaskActivity>> {
    return this.http.post<ApiResponse<TaskActivity>>(this.apiUrl, task);
  }

  updateTask(
    id: number,
    task: TaskActivity,
  ): Observable<ApiResponse<TaskActivity>> {
    return this.http.put<ApiResponse<TaskActivity>>(
      `${this.apiUrl}/${id}`,
      task,
    );
  }

  deleteTask(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getTasksByDateRange(
    startDate: string,
    endDate: string,
  ): Observable<ApiResponse<TaskActivity[]>> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get<ApiResponse<TaskActivity[]>>(
      `${this.apiUrl}/by-date-range`,
      {
        params,
      },
    );
  }
}
