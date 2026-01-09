/**
 * Description: Report Data Models for Analytics and Reporting - defines TypeScript interfaces for various report data structures
 *
 * Author: Dean Ammons
 * Date: November 2025
 */

// Report Data Models for Analytics and Reporting

export interface TimeByClientDto {
  client: string;
  hours: number;
  percentage: number;
}

export interface PhaseHours {
  phase: string;
  hours: number;
}

export interface TimeByProjectDto {
  project: string;
  hours: number;
  phases: PhaseHours[];
}

export interface DailyHoursDto {
  date: string;
  hours: number;
}

export interface TimeByPhaseDto {
  phase: string;
  hours: number;
  percentage: number;
}

export interface ClientHours {
  client: string;
  hours: number;
}

export interface WeeklySummaryDto {
  weekStart: string;
  weekEnd: string;
  totalHours: number;
  clients: ClientHours[];
  change: number; // Week-over-week percentage change
}

export interface TopActivityDto {
  details: string;
  hours: number;
  client: string;
  project: string;
  lastDate: string;
}

export interface MonthlyClientHours {
  client: string;
  hours: number;
}

export interface MonthlyComparisonDto {
  month: string;
  totalHours: number;
  clients: MonthlyClientHours[];
}

export interface DashboardSummaryDto {
  monthHours: number;
  weekHours: number;
  topClient: string;
  topProject: string;
  avgDaily: number;
  clientCount: number;
}

// Date range filter interface
export interface DateRangeFilter {
  startDate: Date | null;
  endDate: Date | null;
}

// User-specific report models (for ADMIN only)
export interface UserSummaryDto {
  username: string;
  totalHours: number;
  billableHours: number;
  nonBillableHours: number;
  taskCount: number;
  avgHoursPerDay: number; // Average billable hours per day
  topClient: string;
  topProject: string;
  lastActivityDate: string;
}

export interface UserHoursDto {
  username: string;
  hours: number;
  percentage: number;
}

export interface UserActivityByDateDto {
  username: string;
  date: string;
  hours: number;
}
