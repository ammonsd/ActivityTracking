/**
 * Description: Dropdown service - provides HTTP client methods for managing dropdown values (clients, projects, phases, etc.)
 *
 * Author: Dean Ammons
 * Date: December 2025
 */

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { DropdownValue } from '../models/task-activity.model';

@Injectable({
  providedIn: 'root',
})
export class DropdownService {
  private apiUrl = `${environment.apiUrl}/dropdowns`;

  constructor(private http: HttpClient) {}

  getClients(): Observable<DropdownValue[]> {
    return this.http.get<DropdownValue[]>(`${this.apiUrl}/clients`);
  }

  getProjects(): Observable<DropdownValue[]> {
    return this.http.get<DropdownValue[]>(`${this.apiUrl}/projects`);
  }

  getPhases(): Observable<DropdownValue[]> {
    return this.http.get<DropdownValue[]>(`${this.apiUrl}/phases`);
  }

  getExpenseTypes(): Observable<DropdownValue[]> {
    return this.http.get<DropdownValue[]>(`${this.apiUrl}/expense-types`);
  }

  getPaymentMethods(): Observable<DropdownValue[]> {
    return this.http.get<DropdownValue[]>(`${this.apiUrl}/payment-methods`);
  }

  getCurrencies(): Observable<DropdownValue[]> {
    return this.http.get<DropdownValue[]>(`${this.apiUrl}/currencies`);
  }

  getVendors(): Observable<DropdownValue[]> {
    return this.http.get<DropdownValue[]>(`${this.apiUrl}/vendors`);
  }

  getAllCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/categories`);
  }

  getAllDropdownValues(): Observable<DropdownValue[]> {
    return this.http.get<DropdownValue[]>(`${this.apiUrl}/all`);
  }

  getValuesByCategory(category: string): Observable<DropdownValue[]> {
    return this.http.get<DropdownValue[]>(
      `${this.apiUrl}/category/${category}`
    );
  }

  addDropdownValue(value: DropdownValue): Observable<DropdownValue> {
    return this.http.post<DropdownValue>(this.apiUrl, value);
  }

  updateDropdownValue(
    id: number,
    value: DropdownValue
  ): Observable<DropdownValue> {
    return this.http.put<DropdownValue>(`${this.apiUrl}/${id}`, value);
  }

  deleteDropdownValue(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
