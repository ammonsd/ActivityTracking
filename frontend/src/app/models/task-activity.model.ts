export interface TaskActivity {
  id?: number;
  taskDate: string;
  client: string;
  project: string;
  phase: string;
  hours: number;
  details: string;
  username?: string;
}

export interface User {
  id?: number;
  username: string;
  firstname: string;
  lastname: string;
  company: string;
  email?: string;
  role: string;
  enabled: boolean;
  forcePasswordUpdate?: boolean;
  accountLocked?: boolean;
  failedLoginAttempts?: number;
}

export interface DropdownValue {
  id?: number;
  category: string;
  categoryName: string;
  subcategory: string;
  itemValue: string;
  displayOrder: number;
  isActive: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  count?: number;
  totalPages?: number;
  currentPage?: number;
  totalElements?: number;
}
