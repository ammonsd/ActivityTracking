import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Expense } from '../models/expense.model';
import { ApiResponse } from '../models/task-activity.model';

@Injectable({
  providedIn: 'root',
})
export class ExpenseService {
  private readonly apiUrl = `${environment.apiUrl}/expenses`;

  constructor(private readonly http: HttpClient) {}

  getAllExpenses(
    page: number = 0,
    size: number = 20,
    client?: string,
    project?: string,
    expenseType?: string,
    status?: string,
    paymentMethod?: string,
    startDate?: string,
    endDate?: string,
    username?: string
  ): Observable<ApiResponse<Expense[]>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (client) params = params.set('client', client);
    if (project) params = params.set('project', project);
    if (expenseType) params = params.set('expenseType', expenseType);
    if (status) params = params.set('status', status);
    if (paymentMethod) params = params.set('paymentMethod', paymentMethod);
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    if (username) params = params.set('username', username);

    return this.http.get<ApiResponse<Expense[]>>(this.apiUrl, { params });
  }

  getExpenseById(id: number): Observable<ApiResponse<Expense>> {
    return this.http.get<ApiResponse<Expense>>(`${this.apiUrl}/${id}`);
  }

  createExpense(expense: Expense): Observable<ApiResponse<Expense>> {
    return this.http.post<ApiResponse<Expense>>(this.apiUrl, expense);
  }

  updateExpense(
    id: number,
    expense: Expense
  ): Observable<ApiResponse<Expense>> {
    return this.http.put<ApiResponse<Expense>>(`${this.apiUrl}/${id}`, expense);
  }

  deleteExpense(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getCurrentWeekExpenses(): Observable<ApiResponse<Expense[]>> {
    return this.http.get<ApiResponse<Expense[]>>(`${this.apiUrl}/current-week`);
  }

  canAccessExpenses(): Observable<ApiResponse<boolean>> {
    return this.http.get<ApiResponse<boolean>>(`${this.apiUrl}/can-access`);
  }
}
