/**
 * Description: Utility functions for Admin Analytics & Reports computations.
 * Mirrors the Angular ReportsService calculation logic in pure TypeScript for the React admin dashboard.
 * Handles billability evaluation, user summary aggregation, and date range presets.
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import type {
    TaskActivity,
    UserSummaryDto,
    UserHoursDto,
    DateRangePreset,
    PhaseDistributionRow,
    StaleProjectDto,
    ClientBillabilityDto,
    ClientTimelineDto,
    DayOfWeekDto,
    TrackingComplianceDto,
    TaskRepetitionDto,
    PeriodDeltaDto,
    PeriodDeltaResult,
    TrendDirection,
} from "../types/reports.types";
import type { DropdownValue } from "../types/dropdown.types";

// ============================================================
// BILLABILITY HELPERS
// ============================================================

/**
 * Checks if a specific dropdown item is marked as non-billable.
 * Returns true (billable) when the item is not found (safe default) or nonBillable flag is false.
 */
function isDropdownBillable(
    dropdowns: DropdownValue[],
    value: string,
    subcategory: string,
    category: string,
): boolean {
    const match = dropdowns.find(
        (d) =>
            d.category === category &&
            d.subcategory === subcategory &&
            d.itemValue === value,
    );
    return !(match?.nonBillable ?? false);
}

/**
 * Determines if a task is billable based on client, project, and phase dropdown flags.
 * A task is billable only when ALL three components are billable (AND logic).
 */
function isTaskBillable(
    task: TaskActivity,
    dropdowns: DropdownValue[],
): boolean {
    return (
        isDropdownBillable(dropdowns, task.client, "CLIENT", "TASK") &&
        isDropdownBillable(dropdowns, task.project, "PROJECT", "TASK") &&
        isDropdownBillable(dropdowns, task.phase, "PHASE", "TASK")
    );
}

// ============================================================
// AGGREGATION HELPERS
// ============================================================

function groupByClient(
    tasks: TaskActivity[],
): { client: string; hours: number }[] {
    const map = new Map<string, number>();
    for (const t of tasks) {
        map.set(t.client, (map.get(t.client) ?? 0) + t.hours);
    }
    return Array.from(map.entries())
        .map(([client, hours]) => ({ client, hours }))
        .sort((a, b) => b.hours - a.hours);
}

function groupByProject(
    tasks: TaskActivity[],
): { project: string; hours: number }[] {
    const map = new Map<string, number>();
    for (const t of tasks) {
        map.set(t.project, (map.get(t.project) ?? 0) + t.hours);
    }
    return Array.from(map.entries())
        .map(([project, hours]) => ({ project, hours }))
        .sort((a, b) => b.hours - a.hours);
}

function round1(n: number): number {
    return Math.round(n * 10) / 10;
}

// ============================================================
// USER ANALYSIS COMPUTATIONS
// ============================================================

/**
 * Computes per-user performance summaries from a flat list of task activities.
 * Results are sorted by total hours descending (most active user first).
 */
export function computeUserSummaries(
    tasks: TaskActivity[],
    dropdowns: DropdownValue[],
): UserSummaryDto[] {
    // Group tasks by username
    const userMap = new Map<string, TaskActivity[]>();
    for (const task of tasks) {
        const username = task.username || "Unknown";
        if (!userMap.has(username)) userMap.set(username, []);
        userMap.get(username)!.push(task);
    }

    const result: UserSummaryDto[] = [];

    for (const [username, userTasks] of userMap) {
        const totalHours = userTasks.reduce((sum, t) => sum + t.hours, 0);
        const taskCount = userTasks.length;

        const billableTasks = userTasks.filter((t) =>
            isTaskBillable(t, dropdowns),
        );
        const nonBillableTasks = userTasks.filter(
            (t) => !isTaskBillable(t, dropdowns),
        );

        const billableHours = billableTasks.reduce(
            (sum, t) => sum + t.hours,
            0,
        );
        const nonBillableHours = nonBillableTasks.reduce(
            (sum, t) => sum + t.hours,
            0,
        );

        // Average based on distinct days with billable entries
        const billableDates = new Set(billableTasks.map((t) => t.taskDate));
        const daysWorked = billableDates.size;
        const avgHoursPerDay = daysWorked > 0 ? billableHours / daysWorked : 0;

        const billabilityRate =
            totalHours > 0 ? (billableHours / totalHours) * 100 : 0;

        const clientGroups = groupByClient(billableTasks);
        const projectGroups = groupByProject(billableTasks);
        const topClient = clientGroups[0]?.client ?? "N/A";
        const topProject = projectGroups[0]?.project ?? "N/A";

        const lastTs = Math.max(
            ...userTasks.map((t) => new Date(t.taskDate).getTime()),
        );
        const lastActivityDate = new Date(lastTs).toISOString().split("T")[0];

        result.push({
            username,
            totalHours: round1(totalHours),
            billableHours: round1(billableHours),
            nonBillableHours: round1(nonBillableHours),
            taskCount,
            avgHoursPerDay: round1(avgHoursPerDay),
            billabilityRate: round1(billabilityRate),
            topClient,
            topProject,
            lastActivityDate,
        });
    }

    result.sort((a, b) => b.totalHours - a.totalHours);
    return result;
}

/**
 * Computes hours breakdown per user for the comparison bar chart.
 * Results are sorted by hours descending.
 */
export function computeUserHours(
    tasks: TaskActivity[],
    dropdowns: DropdownValue[],
): UserHoursDto[] {
    const userMap = new Map<string, { total: number; billable: number }>();

    for (const task of tasks) {
        const username = task.username || "Unknown";
        const current = userMap.get(username) ?? { total: 0, billable: 0 };
        current.total += task.hours;
        if (isTaskBillable(task, dropdowns)) {
            current.billable += task.hours;
        }
        userMap.set(username, current);
    }

    const grandTotal = Array.from(userMap.values()).reduce(
        (sum, v) => sum + v.total,
        0,
    );

    const result: UserHoursDto[] = [];
    for (const [username, { total, billable }] of userMap) {
        result.push({
            username,
            hours: round1(total),
            billableHours: round1(billable),
            percentage: grandTotal > 0 ? round1((total / grandTotal) * 100) : 0,
        });
    }

    result.sort((a, b) => b.hours - a.hours);
    return result;
}

// ============================================================
// NEW REPORT COMPUTATIONS
// ============================================================

/**
 * 1. Phase Distribution by Project
 * For each project, breaks hours down by phase with percentages.
 * Sorted by total hours descending.
 */
export function computePhaseDistribution(
    tasks: TaskActivity[],
): PhaseDistributionRow[] {
    const projectMap = new Map<string, TaskActivity[]>();
    for (const t of tasks) {
        if (!projectMap.has(t.project)) projectMap.set(t.project, []);
        projectMap.get(t.project)!.push(t);
    }

    const result: PhaseDistributionRow[] = [];
    for (const [project, pts] of projectMap) {
        const totalHours = pts.reduce((s, t) => s + t.hours, 0);

        const phaseMap = new Map<string, number>();
        for (const t of pts) {
            phaseMap.set(t.phase, (phaseMap.get(t.phase) ?? 0) + t.hours);
        }
        const phases = Array.from(phaseMap.entries())
            .map(([phase, hours]) => ({
                phase,
                hours: round1(hours),
                percentage: round1((hours / totalHours) * 100),
            }))
            .sort((a, b) => b.hours - a.hours);

        const clientMap = new Map<string, number>();
        for (const t of pts) {
            clientMap.set(t.client, (clientMap.get(t.client) ?? 0) + t.hours);
        }
        const topClient =
            [...clientMap.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] ??
            "N/A";

        result.push({
            project,
            totalHours: round1(totalHours),
            topClient,
            phases,
            topPhase: phases[0]?.phase ?? "N/A",
        });
    }
    result.sort((a, b) => b.totalHours - a.totalHours);
    return result;
}

/**
 * 2. Stale Projects
 * Projects whose last activity date is more than `staleDays` days before today.
 * Sorted by most-stale first.
 */
export function computeStaleProjects(
    tasks: TaskActivity[],
    staleDays: number,
): StaleProjectDto[] {
    const todayMs = new Date().setHours(0, 0, 0, 0);
    const projectMap = new Map<string, TaskActivity[]>();
    for (const t of tasks) {
        if (!projectMap.has(t.project)) projectMap.set(t.project, []);
        projectMap.get(t.project)!.push(t);
    }

    const result: StaleProjectDto[] = [];
    for (const [project, pts] of projectMap) {
        const lastTs = Math.max(
            ...pts.map((t) => new Date(t.taskDate).getTime()),
        );
        const daysSince = Math.floor((todayMs - lastTs) / 86400000);
        if (daysSince < staleDays) continue;

        const totalHours = round1(pts.reduce((s, t) => s + t.hours, 0));
        const clientMap = new Map<string, number>();
        for (const t of pts) {
            clientMap.set(t.client, (clientMap.get(t.client) ?? 0) + t.hours);
        }
        const primaryClient =
            [...clientMap.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] ??
            "N/A";
        const activeUsers = [
            ...new Set(pts.map((t) => t.username || "Unknown")),
        ];

        result.push({
            project,
            totalHours,
            lastActivityDate: new Date(lastTs).toISOString().split("T")[0],
            daysSinceActivity: daysSince,
            primaryClient,
            activeUsers,
        });
    }
    result.sort((a, b) => b.daysSinceActivity - a.daysSinceActivity);
    return result;
}

/**
 * 3. Client Billability Ratio
 * Per client: billable vs non-billable hours with rate.
 * Sorted by total hours descending.
 */
export function computeClientBillability(
    tasks: TaskActivity[],
    dropdowns: DropdownValue[],
): ClientBillabilityDto[] {
    const clientMap = new Map<string, { total: number; billable: number }>();
    for (const t of tasks) {
        const cur = clientMap.get(t.client) ?? { total: 0, billable: 0 };
        cur.total += t.hours;
        if (isTaskBillable(t, dropdowns)) cur.billable += t.hours;
        clientMap.set(t.client, cur);
    }

    return [...clientMap.entries()]
        .map(([client, { total, billable }]) => ({
            client,
            totalHours: round1(total),
            billableHours: round1(billable),
            nonBillableHours: round1(total - billable),
            billabilityRate: total > 0 ? round1((billable / total) * 100) : 0,
        }))
        .sort((a, b) => b.totalHours - a.totalHours);
}

/**
 * 4. Client Activity Timeline
 * Month-by-month hours per client. Months in "YYYY-MM" format.
 * Sorted by total hours descending.
 */
export function computeClientTimeline(
    tasks: TaskActivity[],
): ClientTimelineDto[] {
    const clientMonthMap = new Map<string, Map<string, number>>();
    for (const t of tasks) {
        const month = t.taskDate.substring(0, 7); // "YYYY-MM"
        if (!clientMonthMap.has(t.client))
            clientMonthMap.set(t.client, new Map());
        const mm = clientMonthMap.get(t.client)!;
        mm.set(month, (mm.get(month) ?? 0) + t.hours);
    }

    return [...clientMonthMap.entries()]
        .map(([client, mm]) => {
            const months = [...mm.entries()]
                .map(([month, hours]) => ({ month, hours: round1(hours) }))
                .sort((a, b) => a.month.localeCompare(b.month));
            const totalHours = round1(months.reduce((s, m) => s + m.hours, 0));
            const peakMonth =
                [...mm.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] ?? "";
            return { client, totalHours, months, peakMonth };
        })
        .sort((a, b) => b.totalHours - a.totalHours);
}

/**
 * Counts how many times a given day-of-week (0=Sun…6=Sat) appears between two ISO dates inclusive.
 */
function countDayOccurrences(
    dayOfWeek: number,
    startStr: string,
    endStr: string,
): number {
    const start = new Date(startStr + "T00:00:00");
    const end = new Date(endStr + "T00:00:00");
    const totalDays =
        Math.round((end.getTime() - start.getTime()) / 86400000) + 1;
    const startDay = start.getDay();
    const fullWeeks = Math.floor(totalDays / 7);
    const remainder = totalDays % 7;
    let count = fullWeeks;
    for (let i = 0; i < remainder; i++) {
        if ((startDay + i) % 7 === dayOfWeek) count++;
    }
    return count;
}

/**
 * 5. Hours per Day of Week
 * Aggregate hours grouped by Mon–Sun with average per occurrence.
 * When date boundaries are unknown, falls back to the min/max in the task data.
 */
export function computeDayOfWeekHours(
    tasks: TaskActivity[],
    startDate?: string,
    endDate?: string,
): DayOfWeekDto[] {
    const DAY_NAMES = [
        "Sunday",
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
    ];

    const hoursPerDay = new Array(7).fill(0);
    for (const t of tasks) {
        const d = new Date(t.taskDate + "T00:00:00");
        hoursPerDay[d.getDay()] += t.hours;
    }

    // Determine effective date range for occurrence counting
    let effStart = startDate;
    let effEnd = endDate;
    if (!effStart || !effEnd) {
        const dates = tasks.map((t) => t.taskDate).sort((a, b) => a.localeCompare(b));
        effStart = dates[0];
        effEnd = dates.at(-1);
    }

    return DAY_NAMES.map((dayName, idx) => {
        const occurrences =
            effStart && effEnd ? countDayOccurrences(idx, effStart, effEnd) : 1;
        const totalHours = round1(hoursPerDay[idx]);
        return {
            dayName,
            dayIndex: idx,
            totalHours,
            avgHoursPerOccurrence:
                occurrences > 0 ? round1(totalHours / occurrences) : 0,
            occurrencesInRange: occurrences,
        };
    });
}

/**
 * 6. Tracking Compliance
 * Per user: weekdays (Mon–Fri) in the selected range vs. days with at least one entry.
 * Sorted by compliance rate ascending (worst first).
 * Requires a specific date range; returns empty array if dates are missing.
 */
export function computeTrackingCompliance(
    tasks: TaskActivity[],
    startDate: string,
    endDate: string,
): TrackingComplianceDto[] {
    if (!startDate || !endDate) return [];

    // Enumerate all weekdays in range
    const weekdays: string[] = [];
    const cur = new Date(startDate + "T00:00:00");
    const end = new Date(endDate + "T00:00:00");
    while (cur <= end) {
        const dow = cur.getDay();
        if (dow >= 1 && dow <= 5)
            weekdays.push(cur.toISOString().split("T")[0]);
        cur.setDate(cur.getDate() + 1);
    }

    // Group logged dates per user
    const userDatesMap = new Map<string, Set<string>>();
    for (const t of tasks) {
        const u = t.username || "Unknown";
        if (!userDatesMap.has(u)) userDatesMap.set(u, new Set());
        userDatesMap.get(u)!.add(t.taskDate.split("T")[0]);
    }

    return [...userDatesMap.entries()]
        .map(([username, logged]) => {
            const daysLogged = weekdays.filter((d) => logged.has(d)).length;
            const missed = weekdays.filter((d) => !logged.has(d));
            return {
                username,
                totalWorkdays: weekdays.length,
                daysLogged,
                daysMissing: missed.length,
                complianceRate:
                    weekdays.length > 0
                        ? round1((daysLogged / weekdays.length) * 100)
                        : 100,
                recentMissedDates: missed.slice(-15),
            };
        })
        .sort((a, b) => a.complianceRate - b.complianceRate);
}

/**
 * 7. Task Repetition Rate
 * Most frequently recurring taskId entries across the period.
 * Returns top 50 by occurrence count descending; filters out blank taskIds.
 */
export function computeTaskRepetition(
    tasks: TaskActivity[],
): TaskRepetitionDto[] {
    const taskMap = new Map<string, TaskActivity[]>();
    for (const t of tasks) {
        const tid = (t.taskId || "").trim();
        if (!tid) continue;
        if (!taskMap.has(tid)) taskMap.set(tid, []);
        taskMap.get(tid)!.push(t);
    }

    return [...taskMap.entries()]
        .map(([taskId, entries]) => {
            const totalHours = round1(entries.reduce((s, e) => s + e.hours, 0));
            const users = [
                ...new Set(entries.map((e) => e.username || "Unknown")),
            ];
            const clientMap = new Map<string, number>();
            const projectMap = new Map<string, number>();
            for (const e of entries) {
                clientMap.set(
                    e.client,
                    (clientMap.get(e.client) ?? 0) + e.hours,
                );
                projectMap.set(
                    e.project,
                    (projectMap.get(e.project) ?? 0) + e.hours,
                );
            }
            const topClient =
                [...clientMap.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] ??
                "N/A";
            const topProject =
                [...projectMap.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] ??
                "N/A";
            const sampleDetails =
                entries.find((e) => e.taskDetails)?.taskDetails ?? "";

            return {
                taskId,
                occurrences: entries.length,
                totalHours,
                avgHoursPerOccurrence:
                    entries.length > 0
                        ? round1(totalHours / entries.length)
                        : 0,
                uniqueUsers: users.length,
                topClient,
                topProject,
                sampleDetails,
            };
        })
        .sort((a, b) => b.occurrences - a.occurrences)
        .slice(0, 50);
}

/**
 * 8. Period-over-Period Delta
 * Compares current vs prior period hours per user and per client.
 * trend: "new" = only in current, "dropped" = only in prior, "up/down/flat" = in both.
 */
export function computePeriodDelta(
    currentTasks: TaskActivity[],
    priorTasks: TaskActivity[],
    currentLabel: string,
    priorLabel: string,
): PeriodDeltaResult {
    function buildDeltaDtos(
        getKey: (t: TaskActivity) => string,
    ): PeriodDeltaDto[] {
        const cur = new Map<string, number>();
        for (const t of currentTasks) {
            const k = getKey(t);
            cur.set(k, (cur.get(k) ?? 0) + t.hours);
        }
        const prior = new Map<string, number>();
        for (const t of priorTasks) {
            const k = getKey(t);
            prior.set(k, (prior.get(k) ?? 0) + t.hours);
        }

        const keys = new Set([...cur.keys(), ...prior.keys()]);
        return [...keys]
            .map((name) => {
                const c = round1(cur.get(name) ?? 0);
                const p = round1(prior.get(name) ?? 0);
                const delta = round1(c - p);
                const deltaPercent = p > 0 ? round1(((c - p) / p) * 100) : null;
                let trend: TrendDirection;
                if (p === 0) trend = "new";
                else if (c === 0) trend = "dropped";
                else if (delta > 0) trend = "up";
                else if (delta < 0) trend = "down";
                else trend = "flat";
                return {
                    name,
                    currentHours: c,
                    priorHours: p,
                    delta,
                    deltaPercent,
                    trend,
                } satisfies PeriodDeltaDto;
            })
            .sort((a, b) => Math.abs(b.delta) - Math.abs(a.delta));
    }

    return {
        byUser: buildDeltaDtos((t) => t.username || "Unknown"),
        byClient: buildDeltaDtos((t) => t.client),
        currentLabel,
        priorLabel,
    };
}

// ============================================================
// DATE RANGE PRESETS
// ============================================================

export interface DateRange {
    startDate: string | undefined;
    endDate: string | undefined;
    label: string;
}

function toISO(d: Date): string {
    return d.toISOString().split("T")[0];
}

/**
 * Returns ISO date strings for a given named preset.
 * Returns undefined dates for "allTime" (no filter applied).
 */
export function getDateRangeForPreset(preset: DateRangePreset): DateRange {
    const today = new Date();

    switch (preset) {
        case "currentWeek": {
            const day = today.getDay();
            const diff = day === 0 ? -6 : 1 - day;
            const monday = new Date(
                today.getFullYear(),
                today.getMonth(),
                today.getDate() + diff,
            );
            const sunday = new Date(
                monday.getFullYear(),
                monday.getMonth(),
                monday.getDate() + 6,
            );
            return {
                startDate: toISO(monday),
                endDate: toISO(sunday),
                label: "Current Week",
            };
        }
        case "currentMonth": {
            const start = new Date(today.getFullYear(), today.getMonth(), 1);
            const end = new Date(today.getFullYear(), today.getMonth() + 1, 0);
            return {
                startDate: toISO(start),
                endDate: toISO(end),
                label: "This Month",
            };
        }
        case "lastMonth": {
            const start = new Date(
                today.getFullYear(),
                today.getMonth() - 1,
                1,
            );
            const end = new Date(today.getFullYear(), today.getMonth(), 0);
            return {
                startDate: toISO(start),
                endDate: toISO(end),
                label: "Last Month",
            };
        }
        case "last3Months": {
            const start = new Date(
                today.getFullYear(),
                today.getMonth() - 3,
                1,
            );
            const end = new Date(today.getFullYear(), today.getMonth() + 1, 0);
            return {
                startDate: toISO(start),
                endDate: toISO(end),
                label: "Last 3 Months",
            };
        }
        case "currentYear": {
            const start = new Date(today.getFullYear(), 0, 1);
            const end = new Date(today.getFullYear(), 11, 31);
            return {
                startDate: toISO(start),
                endDate: toISO(end),
                label: "This Year",
            };
        }
        case "allTime":
        default:
            return {
                startDate: undefined,
                endDate: undefined,
                label: "All Time",
            };
    }
}

export const DATE_RANGE_PRESETS: { preset: DateRangePreset; label: string }[] =
    [
        { preset: "currentWeek", label: "Current Week" },
        { preset: "currentMonth", label: "This Month" },
        { preset: "lastMonth", label: "Last Month" },
        { preset: "last3Months", label: "Last 3 Months" },
        { preset: "currentYear", label: "This Year" },
        { preset: "allTime", label: "All Time" },
    ];
