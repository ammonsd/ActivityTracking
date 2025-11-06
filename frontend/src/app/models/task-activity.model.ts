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
  role: string;
  enabled: boolean;
  forcePasswordUpdate?: boolean;
}

export interface DropdownValue {
  id?: number;
  categoryName: string;
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
