/**
 * Description: TypeScript type definitions for Admin Analytics & Reports feature.
 * Mirrors the Angular report model types for consistent data structures.
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

// Raw task activity record from the backend
export interface TaskActivity {
    id: number;
    username: string;
    taskDate: string; // ISO date string "YYYY-MM-DD"
    client: string;
    project: string;
    phase: string;
    taskId: string;
    taskDetails: string;
    hours: number;
}

// API response wrapper
export interface TaskActivityResponse {
    success: boolean;
    message: string;
    data: TaskActivity[];
    totalCount?: number;
    page?: number;
    pageSize?: number;
}

// Per-user performance summary (admin only)
export interface UserSummaryDto {
    username: string;
    totalHours: number;
    billableHours: number;
    nonBillableHours: number;
    taskCount: number;
    avgHoursPerDay: number;
    topClient: string;
    topProject: string;
    lastActivityDate: string;
    billabilityRate: number; // percentage 0-100
}

// Hours breakdown per user for comparison chart
export interface UserHoursDto {
    username: string;
    hours: number;
    billableHours: number;
    percentage: number;
}

// Date range preset options
export type DateRangePreset =
    | "currentWeek"
    | "currentMonth"
    | "lastMonth"
    | "last3Months"
    | "currentYear"
    | "allTime";

// ── Phase Distribution by Project ─────────────────────────────
export interface PhaseHoursBreakdown {
    phase: string;
    hours: number;
    percentage: number;
}

export interface PhaseDistributionRow {
    project: string;
    totalHours: number;
    topClient: string;
    phases: PhaseHoursBreakdown[];
    topPhase: string;
}

// ── Stale Projects ────────────────────────────────────────────
export interface StaleProjectDto {
    project: string;
    totalHours: number;
    lastActivityDate: string;
    daysSinceActivity: number;
    primaryClient: string;
    activeUsers: string[];
}

// ── Client Billability Ratio ──────────────────────────────────
export interface ClientBillabilityDto {
    client: string;
    totalHours: number;
    billableHours: number;
    nonBillableHours: number;
    billabilityRate: number;
}

// ── Client Activity Timeline ──────────────────────────────────
export interface ClientMonthEntry {
    month: string; // "YYYY-MM"
    hours: number;
}

export interface ClientTimelineDto {
    client: string;
    totalHours: number;
    months: ClientMonthEntry[];
    peakMonth: string;
}

// ── Hours per Day of Week ─────────────────────────────────────
export interface DayOfWeekDto {
    dayName: string;
    dayIndex: number; // 0=Sun … 6=Sat
    totalHours: number;
    avgHoursPerOccurrence: number;
    occurrencesInRange: number;
}

// ── Tracking Compliance ───────────────────────────────────────
export interface TrackingComplianceDto {
    username: string;
    totalWorkdays: number;
    daysLogged: number;
    daysMissing: number;
    complianceRate: number;
    recentMissedDates: string[];
}

// ── Task Repetition Rate ──────────────────────────────────────
export interface TaskRepetitionDto {
    taskId: string;
    occurrences: number;
    totalHours: number;
    avgHoursPerOccurrence: number;
    uniqueUsers: number;
    topClient: string;
    topProject: string;
    sampleDetails: string;
}

// ── Period-over-Period Delta ──────────────────────────────────
export type TrendDirection = "up" | "down" | "flat" | "new" | "dropped";

export interface PeriodDeltaDto {
    name: string; // username or client name
    currentHours: number;
    priorHours: number;
    delta: number;
    deltaPercent: number | null; // null when prior = 0
    trend: TrendDirection;
}

export interface PeriodDeltaResult {
    byUser: PeriodDeltaDto[];
    byClient: PeriodDeltaDto[];
    currentLabel: string;
    priorLabel: string;
}
