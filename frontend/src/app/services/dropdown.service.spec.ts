import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { DropdownService } from './dropdown.service';
import { environment } from '../../environments/environment';
import { DropdownValue } from '../models/task-activity.model';

describe('DropdownService', () => {
  let service: DropdownService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/dropdowns`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DropdownService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(DropdownService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getClients', () => {
    it('should retrieve clients from API', () => {
      const mockClients: DropdownValue[] = [
        {
          id: 1,
          categoryName: 'CLIENT',
          itemValue: 'Client A',
          displayOrder: 1,
          isActive: true,
        },
        {
          id: 2,
          categoryName: 'CLIENT',
          itemValue: 'Client B',
          displayOrder: 2,
          isActive: true,
        },
      ];

      service.getClients().subscribe((clients) => {
        expect(clients).toEqual(mockClients);
        expect(clients.length).toBe(2);
      });

      const req = httpMock.expectOne(`${apiUrl}/clients`);
      expect(req.request.method).toBe('GET');
      req.flush(mockClients);
    });

    it('should handle empty clients list', () => {
      service.getClients().subscribe((clients) => {
        expect(clients).toEqual([]);
      });

      const req = httpMock.expectOne(`${apiUrl}/clients`);
      req.flush([]);
    });
  });

  describe('getProjects', () => {
    it('should retrieve projects from API', () => {
      const mockProjects: DropdownValue[] = [
        {
          id: 1,
          categoryName: 'PROJECT',
          itemValue: 'Project X',
          displayOrder: 1,
          isActive: true,
        },
      ];

      service.getProjects().subscribe((projects) => {
        expect(projects).toEqual(mockProjects);
      });

      const req = httpMock.expectOne(`${apiUrl}/projects`);
      expect(req.request.method).toBe('GET');
      req.flush(mockProjects);
    });
  });

  describe('getPhases', () => {
    it('should retrieve phases from API', () => {
      const mockPhases: DropdownValue[] = [
        {
          id: 1,
          categoryName: 'PHASE',
          itemValue: 'Development',
          displayOrder: 1,
          isActive: true,
        },
        {
          id: 2,
          categoryName: 'PHASE',
          itemValue: 'Testing',
          displayOrder: 2,
          isActive: true,
        },
      ];

      service.getPhases().subscribe((phases) => {
        expect(phases).toEqual(mockPhases);
        expect(phases.length).toBe(2);
      });

      const req = httpMock.expectOne(`${apiUrl}/phases`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPhases);
    });
  });

  describe('addDropdownValue', () => {
    it('should add new dropdown value via POST', () => {
      const newValue: DropdownValue = {
        categoryName: 'CLIENT',
        itemValue: 'New Client',
        displayOrder: 3,
        isActive: true,
      };

      const createdValue: DropdownValue = {
        id: 3,
        ...newValue,
      };

      service.addDropdownValue(newValue).subscribe((value) => {
        expect(value).toEqual(createdValue);
        expect(value.id).toBe(3);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(newValue);
      req.flush(createdValue);
    });

    it('should handle errors when adding dropdown value', () => {
      const newValue: DropdownValue = {
        categoryName: 'CLIENT',
        itemValue: '',
        displayOrder: 1,
        isActive: true,
      };

      service.addDropdownValue(newValue).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(400);
        },
      });

      const req = httpMock.expectOne(apiUrl);
      req.flush(
        { message: 'Invalid value' },
        { status: 400, statusText: 'Bad Request' }
      );
    });
  });

  describe('updateDropdownValue', () => {
    it('should update dropdown value via PUT', () => {
      const updatedValue: DropdownValue = {
        id: 1,
        categoryName: 'CLIENT',
        itemValue: 'Updated Client',
        displayOrder: 1,
        isActive: false,
      };

      service.updateDropdownValue(1, updatedValue).subscribe((value) => {
        expect(value).toEqual(updatedValue);
        expect(value.isActive).toBe(false);
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updatedValue);
      req.flush(updatedValue);
    });

    it('should handle 404 when updating non-existent value', () => {
      const updatedValue: DropdownValue = {
        id: 999,
        categoryName: 'CLIENT',
        itemValue: 'Does Not Exist',
        displayOrder: 1,
        isActive: true,
      };

      service.updateDropdownValue(999, updatedValue).subscribe({
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

  describe('deleteDropdownValue', () => {
    it('should delete dropdown value via DELETE', () => {
      service.deleteDropdownValue(1).subscribe((response) => {
        expect(response).toBeUndefined();
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('should handle 404 when deleting non-existent value', () => {
      service.deleteDropdownValue(999).subscribe({
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
      service.deleteDropdownValue(1).subscribe({
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
});
