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
