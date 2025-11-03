import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { UserService } from './user.service';
import { environment } from '../../environments/environment';
import { User, ApiResponse } from '../models/task-activity.model';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/users`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UserService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getAllUsers', () => {
    it('should retrieve all users from API', () => {
      const mockResponse: ApiResponse<User[]> = {
        success: true,
        message: 'Users retrieved',
        data: [
          {
            id: 1,
            username: 'user1',
            firstname: 'John',
            lastname: 'Doe',
            company: 'Acme Corp',
            role: 'USER',
            enabled: true,
          },
          {
            id: 2,
            username: 'admin1',
            firstname: 'Jane',
            lastname: 'Admin',
            company: 'Acme Corp',
            role: 'ADMIN',
            enabled: true,
          },
        ],
        count: 2,
      };

      service.getAllUsers().subscribe((response) => {
        expect(response).toEqual(mockResponse);
        expect(response.data.length).toBe(2);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should handle empty users list', () => {
      const mockResponse: ApiResponse<User[]> = {
        success: true,
        message: 'No users',
        data: [],
        count: 0,
      };

      service.getAllUsers().subscribe((response) => {
        expect(response.data).toEqual([]);
      });

      const req = httpMock.expectOne(apiUrl);
      req.flush(mockResponse);
    });
  });

  describe('getUserById', () => {
    it('should retrieve user by ID', () => {
      const mockResponse: ApiResponse<User> = {
        success: true,
        message: 'User found',
        data: {
          id: 1,
          username: 'user1',
          firstname: 'John',
          lastname: 'Doe',
          company: 'Acme Corp',
          role: 'USER',
          enabled: true,
        },
      };

      service.getUserById(1).subscribe((response) => {
        expect(response).toEqual(mockResponse);
        expect(response.data.id).toBe(1);
        expect(response.data.username).toBe('user1');
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should handle 404 for non-existent user', () => {
      service.getUserById(999).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/999`);
      req.flush(
        { message: 'User not found' },
        { status: 404, statusText: 'Not Found' }
      );
    });
  });

  describe('createUser', () => {
    it('should create new user via POST', () => {
      const newUser: User = {
        username: 'newuser',
        firstname: 'New',
        lastname: 'User',
        company: 'Test Inc',
        role: 'USER',
        enabled: true,
      };

      const mockResponse: ApiResponse<User> = {
        success: true,
        message: 'User created',
        data: {
          id: 3,
          ...newUser,
        },
      };

      service.createUser(newUser).subscribe((response) => {
        expect(response).toEqual(mockResponse);
        expect(response.data.id).toBe(3);
        expect(response.data.username).toBe('newuser');
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(newUser);
      req.flush(mockResponse);
    });

    it('should handle validation errors', () => {
      const invalidUser: User = {
        username: '',
        firstname: '',
        lastname: '',
        company: '',
        role: '',
        enabled: false,
      };

      service.createUser(invalidUser).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(400);
        },
      });

      const req = httpMock.expectOne(apiUrl);
      req.flush(
        { message: 'Invalid user data' },
        { status: 400, statusText: 'Bad Request' }
      );
    });

    it('should handle duplicate username error', () => {
      const duplicateUser: User = {
        username: 'existinguser',
        firstname: 'Test',
        lastname: 'User',
        company: 'Test Inc',
        role: 'USER',
        enabled: true,
      };

      service.createUser(duplicateUser).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(409);
        },
      });

      const req = httpMock.expectOne(apiUrl);
      req.flush(
        { message: 'Username already exists' },
        { status: 409, statusText: 'Conflict' }
      );
    });
  });

  describe('updateUser', () => {
    it('should update user via PUT', () => {
      const updatedUser: User = {
        id: 1,
        username: 'user1',
        firstname: 'John',
        lastname: 'Updated',
        company: 'New Corp',
        role: 'ADMIN',
        enabled: true,
      };

      const mockResponse: ApiResponse<User> = {
        success: true,
        message: 'User updated',
        data: updatedUser,
      };

      service.updateUser(1, updatedUser).subscribe((response) => {
        expect(response).toEqual(mockResponse);
        expect(response.data.lastname).toBe('Updated');
        expect(response.data.role).toBe('ADMIN');
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updatedUser);
      req.flush(mockResponse);
    });

    it('should handle 404 when updating non-existent user', () => {
      const user: User = {
        id: 999,
        username: 'nonexistent',
        firstname: 'Test',
        lastname: 'User',
        company: 'Test Inc',
        role: 'USER',
        enabled: true,
      };

      service.updateUser(999, user).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/999`);
      req.flush(
        { message: 'User not found' },
        { status: 404, statusText: 'Not Found' }
      );
    });
  });

  describe('deleteUser', () => {
    it('should delete user via DELETE', () => {
      service.deleteUser(1).subscribe((response) => {
        expect(response).toBeUndefined();
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('should handle 404 when deleting non-existent user', () => {
      service.deleteUser(999).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/999`);
      req.flush(
        { message: 'User not found' },
        { status: 404, statusText: 'Not Found' }
      );
    });

    it('should handle 403 when unauthorized to delete', () => {
      service.deleteUser(1).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(403);
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      req.flush(
        { message: 'Forbidden - Cannot delete user' },
        { status: 403, statusText: 'Forbidden' }
      );
    });

    it('should handle error when deleting self', () => {
      service.deleteUser(1).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(400);
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      req.flush(
        { message: 'Cannot delete yourself' },
        { status: 400, statusText: 'Bad Request' }
      );
    });
  });
});
