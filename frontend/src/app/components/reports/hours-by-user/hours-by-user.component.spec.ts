import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HoursByUserComponent } from './hours-by-user.component';

describe('HoursByUserComponent', () => {
  let component: HoursByUserComponent;
  let fixture: ComponentFixture<HoursByUserComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HoursByUserComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HoursByUserComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
