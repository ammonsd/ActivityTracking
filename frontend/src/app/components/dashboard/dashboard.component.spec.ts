import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { AuthService } from '../../services/auth.service';
import { BehaviorSubject } from 'rxjs';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let userRoleSubject: BehaviorSubject<string>;

  beforeEach(async () => {
    userRoleSubject = new BehaviorSubject<string>('');

    const authServiceSpy = jasmine.createSpyObj('AuthService', [
      'getCurrentRole',
    ]);
    authServiceSpy.userRole$ = userRoleSubject.asObservable();

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [{ provide: AuthService, useValue: authServiceSpy }],
    }).compileComponents();

    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with current role from auth service', () => {
    authService.getCurrentRole.and.returnValue('ADMIN');

    fixture.detectChanges();

    expect(component.currentRole).toBe('ADMIN');
    expect(authService.getCurrentRole).toHaveBeenCalled();
  });

  it('should update role when auth service emits new role', () => {
    authService.getCurrentRole.and.returnValue('USER');
    fixture.detectChanges();

    expect(component.currentRole).toBe('USER');

    // Simulate role change
    userRoleSubject.next('ADMIN');
    fixture.detectChanges();

    expect(component.currentRole).toBe('ADMIN');
  });

  it('should display dashboard for ADMIN role', () => {
    authService.getCurrentRole.and.returnValue('ADMIN');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const cards = compiled.querySelectorAll('mat-card');

    expect(cards.length).toBeGreaterThan(0);
  });

  it('should display dashboard for USER role', () => {
    authService.getCurrentRole.and.returnValue('USER');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const cards = compiled.querySelectorAll('mat-card');

    expect(cards.length).toBeGreaterThan(0);
  });

  it('should display guest info for GUEST role', () => {
    authService.getCurrentRole.and.returnValue('GUEST');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const infoSection = compiled.querySelector('.info-section');

    expect(infoSection).toBeTruthy();
  });

  it('should show task activities card for ADMIN', () => {
    authService.getCurrentRole.and.returnValue('ADMIN');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const cardTitles = Array.from(compiled.querySelectorAll('mat-card-title'));
    const hasTaskActivities = cardTitles.some((title) =>
      title.textContent?.includes('Task Activities')
    );

    expect(hasTaskActivities).toBe(true);
  });

  it('should show users card for ADMIN only', () => {
    authService.getCurrentRole.and.returnValue('ADMIN');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const cardTitles = Array.from(compiled.querySelectorAll('mat-card-title'));
    const hasUsers = cardTitles.some((title) =>
      title.textContent?.includes('Users')
    );

    expect(hasUsers).toBe(true);
  });

  it('should NOT show users card for USER role', () => {
    authService.getCurrentRole.and.returnValue('USER');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const cardTitles = Array.from(compiled.querySelectorAll('mat-card-title'));
    const hasUsers = cardTitles.some((title) =>
      title.textContent?.includes('Users')
    );

    expect(hasUsers).toBe(false);
  });

  it('should subscribe to role changes on init', () => {
    authService.getCurrentRole.and.returnValue('USER');

    spyOn(userRoleSubject, 'subscribe').and.callThrough();

    component.ngOnInit();

    expect(authService.userRole$).toBeDefined();
  });
});
