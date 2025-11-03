import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TaskListComponent } from './task-list.component';
import { TaskActivityService } from '../../services/task-activity.service';
import { AuthService } from '../../services/auth.service';
import { of, throwError } from 'rxjs';
import { ApiResponse, TaskActivity } from '../../models/task-activity.model';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

describe('TaskListComponent', () => {
  let component: TaskListComponent;
  let fixture: ComponentFixture<TaskListComponent>;
  let taskService: jasmine.SpyObj<TaskActivityService>;
  let authService: jasmine.SpyObj<AuthService>;

  const mockTasksResponse: ApiResponse<TaskActivity[]> = {
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
        details: 'More work',
        username: 'user2',
      },
    ],
    count: 2,
  };

  beforeEach(async () => {
    const taskServiceSpy = jasmine.createSpyObj('TaskActivityService', [
      'getAllTasks',
      'deleteTask',
    ]);
    const authServiceSpy = jasmine.createSpyObj('AuthService', [
      'getCurrentRole',
    ]);

    await TestBed.configureTestingModule({
      imports: [TaskListComponent],
      providers: [
        { provide: TaskActivityService, useValue: taskServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        provideNoopAnimations(),
      ],
    }).compileComponents();

    taskService = TestBed.inject(
      TaskActivityService
    ) as jasmine.SpyObj<TaskActivityService>;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;

    fixture = TestBed.createComponent(TaskListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load tasks on initialization', () => {
    taskService.getAllTasks.and.returnValue(of(mockTasksResponse));
    authService.getCurrentRole.and.returnValue('ADMIN');

    fixture.detectChanges();

    expect(taskService.getAllTasks).toHaveBeenCalled();
    expect(component.tasks).toEqual(mockTasksResponse.data);
    expect(component.loading).toBe(false);
  });

  it('should handle error when loading tasks', () => {
    const errorResponse = { status: 500, statusText: 'Server Error' };
    taskService.getAllTasks.and.returnValue(throwError(() => errorResponse));
    authService.getCurrentRole.and.returnValue('ADMIN');

    spyOn(console, 'error');

    fixture.detectChanges();

    expect(component.loading).toBe(false);
    expect(console.error).toHaveBeenCalled();
  });

  it('should set currentRole from auth service', () => {
    taskService.getAllTasks.and.returnValue(of(mockTasksResponse));
    authService.getCurrentRole.and.returnValue('ADMIN');

    fixture.detectChanges();

    expect(component.currentRole).toBe('ADMIN');
  });

  it('should display tasks in table', () => {
    taskService.getAllTasks.and.returnValue(of(mockTasksResponse));
    authService.getCurrentRole.and.returnValue('ADMIN');

    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const rows = compiled.querySelectorAll('mat-row');

    expect(rows.length).toBe(2);
  });

  it('should show loading spinner when loading', () => {
    component.loading = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const spinner = compiled.querySelector('mat-spinner');

    expect(spinner).toBeTruthy();
  });

  it('should not show loading spinner when not loading', () => {
    taskService.getAllTasks.and.returnValue(of(mockTasksResponse));
    authService.getCurrentRole.and.returnValue('ADMIN');

    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const spinner = compiled.querySelector('mat-spinner');

    expect(spinner).toBeFalsy();
  });

  it('should handle empty tasks list', () => {
    const emptyResponse: ApiResponse<TaskActivity[]> = {
      success: true,
      message: 'No tasks',
      data: [],
      count: 0,
    };
    taskService.getAllTasks.and.returnValue(of(emptyResponse));
    authService.getCurrentRole.and.returnValue('ADMIN');

    fixture.detectChanges();

    expect(component.tasks.length).toBe(0);
  });

  it('should have correct column definitions for ADMIN', () => {
    taskService.getAllTasks.and.returnValue(of(mockTasksResponse));
    authService.getCurrentRole.and.returnValue('ADMIN');

    fixture.detectChanges();

    expect(component.displayedColumns).toContain('actions');
  });

  it('should call deleteTask when delete is triggered', () => {
    taskService.getAllTasks.and.returnValue(of(mockTasksResponse));
    taskService.deleteTask.and.returnValue(of(undefined));
    authService.getCurrentRole.and.returnValue('ADMIN');

    spyOn(globalThis, 'confirm').and.returnValue(true);

    fixture.detectChanges();

    const taskToDelete = mockTasksResponse.data[0];
    component.deleteTask(taskToDelete);

    expect(taskService.deleteTask).toHaveBeenCalledWith(1);
  });
});
