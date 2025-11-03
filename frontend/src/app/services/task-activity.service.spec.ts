import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TaskActivityService } from './task-activity.service';
import { environment } from '../../environments/environment';
import { TaskActivity, ApiResponse } from '../models/task-activity.model';

describe('TaskActivityService', () => {
  let service: TaskActivityService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/task-activities`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TaskActivityService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(TaskActivityService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getAllTasks', () => {
    it('should retrieve all tasks from API', () => {
      const mockResponse: ApiResponse<TaskActivity[]> = {
        success: true,
        message: 'Tasks retrieved',
        data: [
          {
            id: 1,
            taskDate: '2025-11-01',
            client: 'Client A',
            project: 'Project X',
            phase: 'Development',
            hours: 8,
            details: 'Test work',
            username: 'user1',
          },
          {
            id: 2,
            taskDate: '2025-11-02',
            client: 'Client B',
            project: 'Project Y',
            phase: 'Testing',
            hours: 4,
            details: 'Test work 2',
            username: 'user2',
          },
        ],
        count: 2,
      };

      service.getAllTasks().subscribe((response) => {
        expect(response).toEqual(mockResponse);
        expect(response.data.length).toBe(2);
      });

      const req = httpMock.expectOne((request) => {
        return request.url === apiUrl;
      });
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should handle empty task list', () => {
      const mockResponse: ApiResponse<TaskActivity[]> = {
        success: true,
        message: 'No tasks',
        data: [],
        count: 0,
      };

      service.getAllTasks().subscribe((response) => {
        expect(response.data).toEqual([]);
      });

      const req = httpMock.expectOne((request) => {
        return request.url === apiUrl;
      });
      req.flush(mockResponse);
    });

    it('should pass pagination parameters', () => {
      const mockResponse: ApiResponse<TaskActivity[]> = {
        success: true,
        message: 'Tasks retrieved',
        data: [],
        count: 0,
      };

      service.getAllTasks(1, 10).subscribe();

      const req = httpMock.expectOne((request) => {
        return (
          request.url === apiUrl &&
          request.params.get('page') === '1' &&
          request.params.get('size') === '10'
        );
      });
      req.flush(mockResponse);
    });
  });

  describe('getTaskById', () => {
    it('should retrieve task by ID', () => {
      const mockResponse: ApiResponse<TaskActivity> = {
        success: true,
        message: 'Task found',
        data: {
          id: 1,
          taskDate: '2025-11-01',
          client: 'Client A',
          project: 'Project X',
          phase: 'Development',
          hours: 8,
          details: 'Test work',
          username: 'user1',
        },
      };

      service.getTaskById(1).subscribe((response) => {
        expect(response).toEqual(mockResponse);
        expect(response.data.id).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should handle 404 for non-existent task', () => {
      service.getTaskById(999).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/999`);
      req.flush(
        { message: 'Not found' },
        { status: 404, statusText: 'Not Found' }
      );
    });
  });

  describe('createTask', () => {
    it('should create new task via POST', () => {
      const newTask: TaskActivity = {
        taskDate: '2025-11-03',
        client: 'Client C',
        project: 'Project Z',
        phase: 'Planning',
        hours: 6,
        details: 'New task',
      };

      const mockResponse: ApiResponse<TaskActivity> = {
        success: true,
        message: 'Task created',
        data: {
          id: 3,
          ...newTask,
          username: 'currentUser',
        },
      };

      service.createTask(newTask).subscribe((response) => {
        expect(response).toEqual(mockResponse);
        expect(response.data.id).toBe(3);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(newTask);
      req.flush(mockResponse);
    });

    it('should handle validation errors', () => {
      const invalidTask: TaskActivity = {
        taskDate: '',
        client: '',
        project: '',
        phase: '',
        hours: -1,
        details: '',
      };

      service.createTask(invalidTask).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(400);
        },
      });

      const req = httpMock.expectOne(apiUrl);
      req.flush(
        { message: 'Invalid task data' },
        { status: 400, statusText: 'Bad Request' }
      );
    });
  });

  describe('updateTask', () => {
    it('should update task via PUT', () => {
      const updatedTask: TaskActivity = {
        id: 1,
        taskDate: '2025-11-01',
        client: 'Updated Client',
        project: 'Updated Project',
        phase: 'Updated Phase',
        hours: 10,
        details: 'Updated details',
        username: 'user1',
      };

      const mockResponse: ApiResponse<TaskActivity> = {
        success: true,
        message: 'Task updated',
        data: updatedTask,
      };

      service.updateTask(1, updatedTask).subscribe((response) => {
        expect(response).toEqual(mockResponse);
        expect(response.data.client).toBe('Updated Client');
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updatedTask);
      req.flush(mockResponse);
    });

    it('should handle 404 when updating non-existent task', () => {
      const task: TaskActivity = {
        id: 999,
        taskDate: '2025-11-01',
        client: 'Client',
        project: 'Project',
        phase: 'Phase',
        hours: 8,
        details: 'Details',
      };

      service.updateTask(999, task).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/999`);
      req.flush(
        { message: 'Not found' },
        { status: 404, statusText: 'Not Found' }
      );
    });
  });

  describe('deleteTask', () => {
    it('should delete task via DELETE', () => {
      service.deleteTask(1).subscribe((response) => {
        expect(response).toBeUndefined();
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('should handle 404 when deleting non-existent task', () => {
      service.deleteTask(999).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/999`);
      req.flush(
        { message: 'Not found' },
        { status: 404, statusText: 'Not Found' }
      );
    });

    it('should handle 403 when unauthorized to delete', () => {
      service.deleteTask(1).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(403);
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      req.flush(
        { message: 'Forbidden' },
        { status: 403, statusText: 'Forbidden' }
      );
    });
  });

  describe('getTasksByDateRange', () => {
    it('should retrieve tasks within date range', () => {
      const mockResponse: ApiResponse<TaskActivity[]> = {
        success: true,
        message: 'Tasks found',
        data: [
          {
            id: 1,
            taskDate: '2025-11-01',
            client: 'Client A',
            project: 'Project X',
            phase: 'Development',
            hours: 8,
            details: 'Test work',
            username: 'user1',
          },
        ],
        count: 1,
      };

      service
        .getTasksByDateRange('2025-11-01', '2025-11-30')
        .subscribe((response) => {
          expect(response).toEqual(mockResponse);
          expect(response.data.length).toBe(1);
        });

      const req = httpMock.expectOne((request) => {
        return (
          request.url.includes(`${apiUrl}/date-range`) &&
          request.params.get('startDate') === '2025-11-01' &&
          request.params.get('endDate') === '2025-11-30'
        );
      });

      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });
});
