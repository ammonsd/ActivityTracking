/**
 * Description: User Summary report unit tests - tests for the user summary report component
 *
 * Author: Dean Ammons
 * Date: November 2025
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserSummaryComponent } from './user-summary.component';

describe('UserSummaryComponent', () => {
  let component: UserSummaryComponent;
  let fixture: ComponentFixture<UserSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserSummaryComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UserSummaryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
