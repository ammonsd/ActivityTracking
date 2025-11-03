import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DropdownEditDialogComponent } from './dropdown-edit-dialog.component';

describe('DropdownEditDialogComponent', () => {
  let component: DropdownEditDialogComponent;
  let fixture: ComponentFixture<DropdownEditDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DropdownEditDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DropdownEditDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
