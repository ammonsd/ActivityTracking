/**
 * Description: Reports service - provides HTTP client methods for fetching report data and analytics
 *
 * Author: Dean Ammons
 * Date: November 2025
 */

import { Injectable } from '@angular/core';
import { Observable, map, forkJoin } from 'rxjs';
import {
  TimeByClientDto,
  TimeByProjectDto,
  DailyHoursDto,
  TimeByPhaseDto,
  WeeklySummaryDto,
  TopActivityDto,
  MonthlyComparisonDto,
  DashboardSummaryDto,
  PhaseHours,
  ClientHours,
  UserSummaryDto,
  UserHoursDto,
  UserActivityByDateDto,
} from '../models/report.model';
import { TaskActivityService } from './task-activity.service';
import { TaskActivity, DropdownValue } from '../models/task-activity.model';
import { AuthService } from './auth.service';
import { DropdownService } from './dropdown.service';

@Injectable({
  providedIn: 'root',
})
export class ReportsService {
  private readonly dropdownCache: DropdownValue[] = [];

  constructor(
    private readonly taskActivityService: TaskActivityService,
    private readonly authService: AuthService,
    private readonly dropdownService: DropdownService,
  ) {
    // Load dropdowns on service initialization
    this.loadDropdownsForBillability();
  }

  /**
   * Loads dropdown values into cache for billability checks.
   * Called once during service initialization.
   */
  private loadDropdownsForBillability(): void {
    this.dropdownService.getAllDropdownValues().subscribe({
      next: (dropdowns) => {
        this.dropdownCache.push(...dropdowns);
      },
      error: (err) => {
        console.error('Failed to load dropdowns for billability checks:', err);
      },
    });
  }

  /**
   * Evaluates if a task is billable based on dropdown flags.
   * Uses AND logic (all must be billable) which is equivalent to OR logic for non-billable flags:
   * If ANY component (client/project/phase) is non-billable, returns false.
   * Made public for use in components.
   */
  public isTaskBillable(task: TaskActivity): boolean {
    return (
      this.isBillable(task.client, 'CLIENT', 'TASK') &&
      this.isBillable(task.project, 'PROJECT', 'TASK') &&
      this.isBillable(task.phase, 'PHASE', 'TASK')
    );
  }

  /**
   * Evaluates if an expense is billable based on dropdown flags.
   * Uses AND logic (all must be billable) which is equivalent to OR logic for non-billable flags:
   * If ANY component (client/project/expenseType) is non-billable, returns false.
   * Made public for use in components.
   */
  public isExpenseBillable(expense: {
    client: string;
    project?: string;
    expenseType: string;
  }): boolean {
    return (
      this.isBillable(expense.client, 'CLIENT', 'EXPENSE') &&
      this.isBillable(expense.project || '', 'PROJECT', 'EXPENSE') &&
      this.isBillable(expense.expenseType, 'EXPENSE_TYPE', 'EXPENSE')
    );
  }

  /**
   * Checks if a specific dropdown value is billable.
   * Returns true if dropdown is not found (fail-safe default) or if nonBillable flag is false.
   */
  private isBillable(
    value: string,
    subcategory: string,
    category: string,
  ): boolean {
    const dropdown = this.dropdownCache.find(
      (d) =>
        d.category === category &&
        d.subcategory === subcategory &&
        d.itemValue === value,
    );
    // Return true (billable) if dropdown not found or nonBillable flag is false/undefined
    return !(dropdown?.nonBillable ?? false);
  }

  // Helper method to get date range for current month
  private getCurrentMonthRange(): { startDate: string; endDate: string } {
    const now = new Date();
    const startDate = new Date(now.getFullYear(), now.getMonth(), 1)
      .toISOString()
      .split('T')[0];
    const endDate = new Date(now.getFullYear(), now.getMonth() + 1, 0)
      .toISOString()
      .split('T')[0];
    return { startDate, endDate };
  }

  // Helper method to get date range for current week
  private getCurrentWeekRange(): { startDate: string; endDate: string } {
    const now = new Date();
    const dayOfWeek = now.getDay();
    const diff = now.getDate() - dayOfWeek + (dayOfWeek === 0 ? -6 : 1);
    const monday = new Date(now.setDate(diff));
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);

    return {
      startDate: monday.toISOString().split('T')[0],
      endDate: sunday.toISOString().split('T')[0],
    };
  }

  // Helper method to get date range for last N days
  private getLastNDaysRange(days: number): {
    startDate: string;
    endDate: string;
  } {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(endDate.getDate() - days);

    return {
      startDate: startDate.toISOString().split('T')[0],
      endDate: endDate.toISOString().split('T')[0],
    };
  }

  // Helper method to fetch all data for a date range (with role-based filtering)
  // Backend automatically filters by username for non-admin users
  private fetchTasksForDateRange(
    startDate: string,
    endDate: string,
  ): Observable<TaskActivity[]> {
    // Use getAllTasks which respects role-based filtering on the backend
    // Pass large page size to get all matching records
    // Signature: getAllTasks(page, size, client, project, phase, taskId, startDate, endDate)
    return this.taskActivityService
      .getAllTasks(
        0,
        10000,
        undefined,
        undefined,
        undefined,
        undefined, // taskId — must be explicitly skipped so startDate/endDate land on the correct slots
        startDate,
        endDate,
      )
      .pipe(map((response) => response.data || []));
  }

  getDashboardSummary(
    startDate?: Date,
    endDate?: Date,
  ): Observable<DashboardSummaryDto> {
    const monthRange =
      startDate && endDate
        ? {
            startDate: startDate.toISOString().split('T')[0],
            endDate: endDate.toISOString().split('T')[0],
          }
        : this.getCurrentMonthRange();

    // Always show current week as a reference point
    const weekRange = this.getCurrentWeekRange();

    return forkJoin({
      monthData: this.fetchTasksForDateRange(
        monthRange.startDate,
        monthRange.endDate,
      ),
      weekData: this.fetchTasksForDateRange(
        weekRange.startDate,
        weekRange.endDate,
      ),
    }).pipe(
      map(({ monthData, weekData }) => {
        const monthTasks = monthData;
        const weekTasks = weekData;

        const monthHours = monthTasks.reduce(
          (sum: number, task: TaskActivity) => sum + task.hours,
          0,
        );
        const weekHours = weekTasks.reduce(
          (sum: number, task: TaskActivity) => sum + task.hours,
          0,
        );

        // Get top client and project from month data
        const clientHours = this.groupByClient(monthTasks);
        const projectCounts = this.groupByProject(monthTasks);

        const topClient =
          clientHours.length > 0 ? clientHours[0].client : 'N/A';
        const topProject =
          projectCounts.length > 0 ? projectCounts[0].project : 'N/A';

        // Calculate average daily hours based on days with actual time entries
        const uniqueDates = new Set(
          monthTasks.map((t: TaskActivity) => t.taskDate.split('T')[0]),
        );
        const daysWithEntries = uniqueDates.size;
        const avgDaily = daysWithEntries > 0 ? monthHours / daysWithEntries : 0;

        // Count unique clients
        const clientCount = new Set(
          monthTasks.map((t: TaskActivity) => t.client),
        ).size;

        return {
          monthHours: Math.round(monthHours * 10) / 10,
          weekHours: Math.round(weekHours * 10) / 10,
          topClient,
          topProject,
          avgDaily: Math.round(avgDaily * 10) / 10,
          clientCount,
        };
      }),
    );
  }

  getTimeByClient(
    startDate?: Date,
    endDate?: Date,
  ): Observable<TimeByClientDto[]> {
    const range = this.getDateRange(startDate, endDate);

    return this.fetchTasksForDateRange(range.startDate, range.endDate).pipe(
      map((tasks) => {
        const clientHours = this.groupByClient(tasks);
        const totalHours = clientHours.reduce(
          (sum, item) => sum + item.hours,
          0,
        );

        return clientHours.map((item) => ({
          client: item.client,
          hours: Math.round(item.hours * 10) / 10,
          percentage: Math.round((item.hours / totalHours) * 1000) / 10,
        }));
      }),
    );
  }

  getTimeByProject(
    startDate?: Date,
    endDate?: Date,
    client?: string,
  ): Observable<TimeByProjectDto[]> {
    const range = this.getDateRange(startDate, endDate);

    return this.fetchTasksForDateRange(range.startDate, range.endDate).pipe(
      map((tasks) => {
        if (client) {
          tasks = tasks.filter((t) => t.client === client);
        }

        const projectMap = new Map<string, Map<string, number>>();

        for (const task of tasks) {
          if (!projectMap.has(task.project)) {
            projectMap.set(task.project, new Map<string, number>());
          }
          const phaseMap = projectMap.get(task.project)!;
          const currentHours = phaseMap.get(task.phase) || 0;
          phaseMap.set(task.phase, currentHours + task.hours);
        }

        const result: TimeByProjectDto[] = [];
        for (const [project, phaseMap] of projectMap) {
          const phases: PhaseHours[] = [];
          let totalHours = 0;

          for (const [phase, hours] of phaseMap) {
            phases.push({
              phase,
              hours: Math.round(hours * 10) / 10,
            });
            totalHours += hours;
          }

          phases.sort((a, b) => b.hours - a.hours);
          result.push({
            project,
            hours: Math.round(totalHours * 10) / 10,
            phases,
          });
        }

        result.sort((a, b) => b.hours - a.hours);
        return result;
      }),
    );
  }

  getDailyHours(startDate?: Date, endDate?: Date): Observable<DailyHoursDto[]> {
    const range = this.getDateRange(startDate, endDate);

    return this.fetchTasksForDateRange(range.startDate, range.endDate).pipe(
      map((tasks) => {
        const dailyMap = new Map<string, number>();

        for (const task of tasks) {
          const currentHours = dailyMap.get(task.taskDate) || 0;
          dailyMap.set(task.taskDate, currentHours + task.hours);
        }

        const result: DailyHoursDto[] = [];
        const start = new Date(range.startDate);
        const end = new Date(range.endDate);

        for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
          const dateStr = d.toISOString().split('T')[0];
          result.push({
            date: dateStr,
            hours: Math.round((dailyMap.get(dateStr) || 0) * 10) / 10,
          });
        }

        return result;
      }),
    );
  }

  getTimeByPhase(
    startDate?: Date,
    endDate?: Date,
    project?: string,
  ): Observable<TimeByPhaseDto[]> {
    const range = this.getDateRange(startDate, endDate);

    return this.fetchTasksForDateRange(range.startDate, range.endDate).pipe(
      map((tasks) => {
        if (project) {
          tasks = tasks.filter((t) => t.project === project);
        }

        const phaseMap = new Map<string, number>();
        let totalHours = 0;

        for (const task of tasks) {
          const currentHours = phaseMap.get(task.phase) || 0;
          phaseMap.set(task.phase, currentHours + task.hours);
          totalHours += task.hours;
        }

        const result: TimeByPhaseDto[] = [];
        for (const [phase, hours] of phaseMap) {
          result.push({
            phase,
            hours: Math.round(hours * 10) / 10,
            percentage: Math.round((hours / totalHours) * 1000) / 10,
          });
        }

        result.sort((a, b) => b.hours - a.hours);
        return result;
      }),
    );
  }

  getWeeklySummary(
    startDate?: Date,
    endDate?: Date,
  ): Observable<WeeklySummaryDto[]> {
    let start: Date;
    let end: Date;

    if (startDate && endDate) {
      start = startDate;
      end = endDate;
    } else {
      end = new Date();
      start = new Date();
      start.setDate(end.getDate() - 4 * 7); // Default to 4 weeks
    }

    return this.fetchTasksForDateRange(
      start.toISOString().split('T')[0],
      end.toISOString().split('T')[0],
    ).pipe(
      map((tasks) => {
        const weeklyData = new Map<
          string,
          { start: Date; end: Date; tasks: TaskActivity[] }
        >();

        // Group tasks by week
        for (const task of tasks) {
          const taskDate = new Date(task.taskDate);
          const weekStart = this.getWeekStart(taskDate);
          const weekKey = weekStart.toISOString().split('T')[0];

          if (!weeklyData.has(weekKey)) {
            const weekEnd = new Date(weekStart);
            weekEnd.setDate(weekEnd.getDate() + 6);
            weeklyData.set(weekKey, {
              start: weekStart,
              end: weekEnd,
              tasks: [],
            });
          }
          weeklyData.get(weekKey)!.tasks.push(task);
        }

        const result: WeeklySummaryDto[] = [];
        const sortedWeeks = Array.from(weeklyData.entries());
        sortedWeeks.sort((a, b) => b[1].start.getTime() - a[1].start.getTime());

        for (const [key, weekData] of sortedWeeks) {
          const index = sortedWeeks.findIndex(([k]) => k === key);
          const totalHours = weekData.tasks.reduce(
            (sum, t) => sum + t.hours,
            0,
          );
          const clientHours = this.groupByClient(weekData.tasks);

          let change = 0;
          if (index < sortedWeeks.length - 1) {
            const prevWeekHours = sortedWeeks[index + 1][1].tasks.reduce(
              (sum, t) => sum + t.hours,
              0,
            );
            if (prevWeekHours > 0) {
              change = ((totalHours - prevWeekHours) / prevWeekHours) * 100;
            }
          }

          result.push({
            weekStart: weekData.start.toISOString().split('T')[0],
            weekEnd: weekData.end.toISOString().split('T')[0],
            totalHours: Math.round(totalHours * 10) / 10,
            clients: clientHours.slice(0, 5).map((c) => ({
              client: c.client,
              hours: Math.round(c.hours * 10) / 10,
            })),
            change: Math.round(change * 10) / 10,
          });
        }

        return result;
      }),
    );
  }

  getTopActivities(
    startDate?: Date,
    endDate?: Date,
    minHours?: number,
  ): Observable<TopActivityDto[]> {
    const range = this.getDateRange(startDate, endDate);

    return this.fetchTasksForDateRange(range.startDate, range.endDate).pipe(
      map((tasks) => {
        const activityMap = new Map<
          string,
          {
            hours: number;
            client: string;
            project: string;
            lastDate: string;
          }
        >();

        for (const task of tasks) {
          const key = task.details.trim();
          if (activityMap.has(key)) {
            const existing = activityMap.get(key)!;
            existing.hours += task.hours;
            if (task.taskDate > existing.lastDate) {
              existing.lastDate = task.taskDate;
            }
          } else {
            activityMap.set(key, {
              hours: task.hours,
              client: task.client,
              project: task.project,
              lastDate: task.taskDate,
            });
          }
        }

        const result: TopActivityDto[] = [];
        for (const [details, data] of activityMap) {
          if (!minHours || data.hours >= minHours) {
            result.push({
              details,
              hours: Math.round(data.hours * 10) / 10,
              client: data.client,
              project: data.project,
              lastDate: data.lastDate,
            });
          }
        }

        result.sort((a, b) => b.hours - a.hours);
        return result.slice(0, 10);
      }),
    );
  }

  getMonthlyComparison(
    startDate?: Date,
    endDate?: Date,
  ): Observable<MonthlyComparisonDto[]> {
    let start: Date;
    let end: Date;

    if (startDate && endDate) {
      start = new Date(startDate);
      start.setDate(1); // First day of start month
      end = endDate;
    } else {
      end = new Date();
      start = new Date();
      start.setMonth(end.getMonth() - 6); // Default to 6 months
      start.setDate(1);
    }

    return this.fetchTasksForDateRange(
      start.toISOString().split('T')[0],
      end.toISOString().split('T')[0],
    ).pipe(
      map((tasks) => {
        const monthlyMap = new Map<
          string,
          { tasks: TaskActivity[]; date: Date }
        >();

        for (const task of tasks) {
          const taskDate = new Date(task.taskDate);
          const monthKey = `${taskDate.getFullYear()}-${String(
            taskDate.getMonth() + 1,
          ).padStart(2, '0')}`;

          if (!monthlyMap.has(monthKey)) {
            monthlyMap.set(monthKey, {
              tasks: [],
              date: new Date(taskDate.getFullYear(), taskDate.getMonth(), 1),
            });
          }
          monthlyMap.get(monthKey)!.tasks.push(task);
        }

        const result: MonthlyComparisonDto[] = [];
        const sortedMonths = Array.from(monthlyMap.entries());
        sortedMonths.sort((a, b) => b[1].date.getTime() - a[1].date.getTime());

        for (const [month, data] of sortedMonths) {
          const totalHours = data.tasks.reduce((sum, t) => sum + t.hours, 0);
          const clientHours = this.groupByClient(data.tasks);

          result.push({
            month,
            totalHours: Math.round(totalHours * 10) / 10,
            clients: clientHours.slice(0, 5).map((c) => ({
              client: c.client,
              hours: Math.round(c.hours * 10) / 10,
            })),
          });
        }

        result.reverse();
        return result;
      }),
    );
  }

  // Helper methods
  private getDateRange(
    startDate?: Date,
    endDate?: Date,
  ): { startDate: string; endDate: string } {
    if (startDate && endDate) {
      return {
        startDate: startDate.toISOString().split('T')[0],
        endDate: endDate.toISOString().split('T')[0],
      };
    }
    return this.getCurrentMonthRange();
  }

  private groupByClient(tasks: TaskActivity[]): ClientHours[] {
    const clientMap = new Map<string, number>();

    for (const task of tasks) {
      const currentHours = clientMap.get(task.client) || 0;
      clientMap.set(task.client, currentHours + task.hours);
    }

    const result: ClientHours[] = [];
    for (const [client, hours] of clientMap) {
      result.push({ client, hours });
    }

    result.sort((a, b) => b.hours - a.hours);
    return result;
  }

  private groupByProject(tasks: TaskActivity[]): TimeByProjectDto[] {
    const projectMap = new Map<string, number>();

    for (const task of tasks) {
      const currentHours = projectMap.get(task.project) || 0;
      projectMap.set(task.project, currentHours + task.hours);
    }

    const result: TimeByProjectDto[] = [];
    for (const [project, hours] of projectMap) {
      result.push({ project, hours, phases: [] });
    }

    result.sort((a, b) => b.hours - a.hours);
    return result;
  }

  private getWeekStart(date: Date): Date {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    return new Date(d.setDate(diff));
  }

  // ============================================
  // USER ANALYSIS METHODS (ADMIN ONLY)
  // ============================================

  /**
   * Get summary statistics for each user (ADMIN only)
   * Returns metrics like total hours, task count, averages per user
   */
  getUserSummaries(
    startDate?: Date,
    endDate?: Date,
  ): Observable<UserSummaryDto[]> {
    const range = this.getDateRange(startDate, endDate);

    return this.fetchTasksForDateRange(range.startDate, range.endDate).pipe(
      map((tasks) => {
        // Group tasks by username
        const userMap = new Map<string, TaskActivity[]>();

        for (const task of tasks) {
          const username = task.username || 'Unknown';
          if (!userMap.has(username)) {
            userMap.set(username, []);
          }
          userMap.get(username)!.push(task);
        }

        // Calculate statistics for each user
        const result: UserSummaryDto[] = [];
        for (const [username, userTasks] of userMap) {
          const totalHours = userTasks.reduce((sum, t) => sum + t.hours, 0);
          const taskCount = userTasks.length;

          // Separate billable and non-billable hours using flag-based evaluation
          const billableHours = userTasks
            .filter((t) => this.isTaskBillable(t))
            .reduce((sum, t) => sum + t.hours, 0);
          const nonBillableHours = userTasks
            .filter((t) => !this.isTaskBillable(t))
            .reduce((sum, t) => sum + t.hours, 0);

          // Calculate date range for billable hours only
          const billableDates = new Set(
            userTasks
              .filter((t) => this.isTaskBillable(t))
              .map((t) => t.taskDate),
          );
          const daysWorked = billableDates.size;
          const avgHoursPerDay =
            daysWorked > 0 ? billableHours / daysWorked : 0;

          // Get top client and project (excluding non-billable)
          const billableTasks = userTasks.filter((t) => this.isTaskBillable(t));
          const clientHours = this.groupByClient(billableTasks);
          const projectCounts = this.groupByProject(billableTasks);

          const topClient =
            clientHours.length > 0 ? clientHours[0].client : 'N/A';
          const topProject =
            projectCounts.length > 0 ? projectCounts[0].project : 'N/A';

          // Get last activity date
          const lastActivityDate = Math.max(
            ...userTasks.map((t) => new Date(t.taskDate).getTime()),
          );

          result.push({
            username,
            totalHours: Math.round(totalHours * 10) / 10,
            billableHours: Math.round(billableHours * 10) / 10,
            nonBillableHours: Math.round(nonBillableHours * 10) / 10,
            taskCount,
            avgHoursPerDay: Math.round(avgHoursPerDay * 10) / 10,
            topClient,
            topProject,
            lastActivityDate: new Date(lastActivityDate)
              .toISOString()
              .split('T')[0],
          });
        }

        // Sort by total hours descending
        result.sort((a, b) => b.totalHours - a.totalHours);
        return result;
      }),
    );
  }

  /**
   * Get hours breakdown by user for comparison chart (ADMIN only)
   */
  getHoursByUser(startDate?: Date, endDate?: Date): Observable<UserHoursDto[]> {
    const range = this.getDateRange(startDate, endDate);

    return this.fetchTasksForDateRange(range.startDate, range.endDate).pipe(
      map((tasks) => {
        // Group by username and sum hours
        const userHoursMap = new Map<string, number>();

        for (const task of tasks) {
          const username = task.username || 'Unknown';
          const currentHours = userHoursMap.get(username) || 0;
          userHoursMap.set(username, currentHours + task.hours);
        }

        const totalHours = Array.from(userHoursMap.values()).reduce(
          (sum, h) => sum + h,
          0,
        );

        const result: UserHoursDto[] = [];
        for (const [username, hours] of userHoursMap) {
          result.push({
            username,
            hours: Math.round(hours * 10) / 10,
            percentage: Math.round((hours / totalHours) * 1000) / 10,
          });
        }

        result.sort((a, b) => b.hours - a.hours);
        return result;
      }),
    );
  }

  /**
   * Get user activity over time for timeline visualization (ADMIN only)
   */
  getUserActivityTimeline(
    startDate?: Date,
    endDate?: Date,
  ): Observable<UserActivityByDateDto[]> {
    const range = this.getDateRange(startDate, endDate);

    return this.fetchTasksForDateRange(range.startDate, range.endDate).pipe(
      map((tasks) => {
        // Group by username and date
        const activityMap = new Map<string, Map<string, number>>();

        for (const task of tasks) {
          const username = task.username || 'Unknown';
          if (!activityMap.has(username)) {
            activityMap.set(username, new Map<string, number>());
          }
          const userDates = activityMap.get(username)!;
          const currentHours = userDates.get(task.taskDate) || 0;
          userDates.set(task.taskDate, currentHours + task.hours);
        }

        // Flatten into array
        const result: UserActivityByDateDto[] = [];
        for (const [username, dateMap] of activityMap) {
          for (const [date, hours] of dateMap) {
            result.push({
              username,
              date,
              hours: Math.round(hours * 10) / 10,
            });
          }
        }

        // Sort by date then username
        result.sort((a, b) => {
          const dateCompare = a.date.localeCompare(b.date);
          if (dateCompare !== 0) return dateCompare;
          return a.username.localeCompare(b.username);
        });

        return result;
      }),
    );
  }
}
