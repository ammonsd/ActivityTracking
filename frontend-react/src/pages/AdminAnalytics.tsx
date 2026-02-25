/**
 * Description: Admin Analytics & Reports page for the React Admin Dashboard.
 * Provides comprehensive project-management analytics drawn entirely from task activity data.
 * All computation is client-side; only task-activity and dropdown endpoints are called.
 *
 * Features (10 tabs):
 *   1. User Summary         — Ranked performance table per user with billability rate
 *   2. Hours by User        — Horizontal segmented bar chart (billable / non-billable)
 *   3. Phase Distribution   — Hours per project broken down by phase
 *   4. Stale Projects       — Projects with no recent activity (adjustable threshold)
 *   5. Client Billability   — Billable vs non-billable ratio per client
 *   6. Client Timeline      — Month-by-month heatmap matrix per client
 *   7. Day of Week          — Aggregate + average hours per weekday
 *   8. Tracking Compliance  — Per-user Mon–Fri logging compliance rate (date range required)
 *   9. Task Repetition      — Most frequently recurring task IDs
 *  10. Period Delta          — Comparison of current vs prior period (date range required)
 *
 * Modified by: Dean Ammons - July 2025
 * Change: Exclude GUEST-role users and inactive clients/projects from all analytics reports
 * Reason: GUEST accounts are for demos; inactive entries overwhelm the Stale Projects tab
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import AccountTreeIcon from "@mui/icons-material/AccountTree";
import AnalyticsIcon from "@mui/icons-material/Analytics";
import BarChartIcon from "@mui/icons-material/BarChart";
import CalendarMonthIcon from "@mui/icons-material/CalendarMonth";
import CompareArrowsIcon from "@mui/icons-material/CompareArrows";
import FactCheckIcon from "@mui/icons-material/FactCheck";
import HourglassEmptyIcon from "@mui/icons-material/HourglassEmpty";
import LeaderboardIcon from "@mui/icons-material/Leaderboard";
import PieChartIcon from "@mui/icons-material/PieChart";
import RefreshIcon from "@mui/icons-material/Refresh";
import RepeatIcon from "@mui/icons-material/Repeat";
import TimelineIcon from "@mui/icons-material/Timeline";
import {
    Alert,
    Box,
    Button,
    ButtonGroup,
    Chip,
    CircularProgress,
    Divider,
    Paper,
    Tab,
    Tabs,
    TextField,
    Typography,
} from "@mui/material";
import React, { useCallback, useEffect, useState } from "react";
import { reportsApi } from "../api/reports.api";
import { userManagementApi } from "../api/userManagement.api";
import { HoursByUserChart } from "../components/adminAnalytics/HoursByUserChart";
import { UserSummaryTable } from "../components/adminAnalytics/UserSummaryTable";
import ClientBillabilityTable from "../components/adminAnalytics/ClientBillabilityTable";
import ClientTimelineTable from "../components/adminAnalytics/ClientTimelineTable";
import DayOfWeekChart from "../components/adminAnalytics/DayOfWeekChart";
import PeriodDeltaTable from "../components/adminAnalytics/PeriodDeltaTable";
import PhaseDistributionTable from "../components/adminAnalytics/PhaseDistributionTable";
import StaleProjectsTable from "../components/adminAnalytics/StaleProjectsTable";
import TaskRepetitionTable from "../components/adminAnalytics/TaskRepetitionTable";
import TrackingComplianceTable from "../components/adminAnalytics/TrackingComplianceTable";
import type { DropdownValue } from "../types/dropdown.types";
import type {
    ClientBillabilityDto,
    ClientTimelineDto,
    DateRangePreset,
    DayOfWeekDto,
    PeriodDeltaResult,
    PhaseDistributionRow,
    StaleProjectDto,
    TaskActivity,
    TaskRepetitionDto,
    TrackingComplianceDto,
    UserHoursDto,
    UserSummaryDto,
} from "../types/reports.types";
import {
    DATE_RANGE_PRESETS,
    computeClientBillability,
    computeClientTimeline,
    computeDayOfWeekHours,
    computePeriodDelta,
    computePhaseDistribution,
    computeStaleProjects,
    computeTaskRepetition,
    computeTrackingCompliance,
    computeUserHours,
    computeUserSummaries,
    filterAnalyticsTasks,
    getDateRangeForPreset,
} from "../utils/reportsUtils";

// ── Helper: compute prior period date range ──────────────────────────────────

interface PriorPeriod {
    priorStart: string;
    priorEnd: string;
    label: string;
    currentLabel: string;
}

/**
 * Given a current start/end date, returns the immediately preceding period of equal length.
 * Returns null when no specific range is supplied (e.g. "All Time" preset).
 */
function getPriorPeriodDates(
    start: string,
    end: string,
): PriorPeriod | null {
    if (!start || !end) return null;
    const startMs = new Date(start + "T00:00:00").getTime();
    const endMs = new Date(end + "T00:00:00").getTime();
    const durationMs = endMs - startMs;
    const priorEndMs = startMs - 86_400_000;
    const priorStartMs = priorEndMs - durationMs;
    const priorStart = new Date(priorStartMs).toISOString().split("T")[0];
    const priorEnd = new Date(priorEndMs).toISOString().split("T")[0];
    return {
        priorStart,
        priorEnd,
        label: `${priorStart} – ${priorEnd}`,
        currentLabel: `${start} – ${end}`,
    };
}

// ── TabPanel ─────────────────────────────────────────────────────────────────

interface TabPanelProps {
    children?: React.ReactNode;
    index: number;
    value: number;
}

const TabPanel: React.FC<TabPanelProps> = ({ children, value, index }) => (
    <div role="tabpanel" hidden={value !== index} aria-labelledby={`analytics-tab-${index}`}>
        {value === index && <Box sx={{ pt: 3 }}>{children}</Box>}
    </div>
);

// ── Page component ────────────────────────────────────────────────────────────

export const AdminAnalytics: React.FC = () => {
    const [activeTab, setActiveTab] = useState(0);
    const [activePreset, setActivePreset] =
        useState<DateRangePreset>("currentMonth");

    // Date range state — initialised to current month
    const initRange = getDateRangeForPreset("currentMonth");
    const [startDate, setStartDate] = useState<string>(
        initRange.startDate ?? "",
    );
    const [endDate, setEndDate] = useState<string>(initRange.endDate ?? "");

    // Raw task storage (needed to re-compute stale proj when threshold slider moves)
    const [rawTasks, setRawTasks] = useState<TaskActivity[]>([]);

    // Stale projects threshold (local UI control — no re-fetch needed)
    const [staleDays, setStaleDays] = useState(30);

    // Derived data state — original reports
    const [userSummaries, setUserSummaries] = useState<UserSummaryDto[]>([]);
    const [userHours, setUserHours] = useState<UserHoursDto[]>([]);

    // Derived data state — 8 new reports
    const [phaseDistribution, setPhaseDistribution] = useState<PhaseDistributionRow[]>([]);
    const [staleProjects, setStaleProjects] = useState<StaleProjectDto[]>([]);
    const [clientBillability, setClientBillability] = useState<ClientBillabilityDto[]>([]);
    const [clientTimeline, setClientTimeline] = useState<ClientTimelineDto[]>([]);
    const [dayOfWeekHours, setDayOfWeekHours] = useState<DayOfWeekDto[]>([]);
    const [trackingCompliance, setTrackingCompliance] = useState<TrackingComplianceDto[]>([]);
    const [taskRepetition, setTaskRepetition] = useState<TaskRepetitionDto[]>([]);
    const [periodDelta, setPeriodDelta] = useState<PeriodDeltaResult | null>(null);

    // Support data
    const [dropdowns, setDropdowns] = useState<DropdownValue[]>([]);
    // GUEST usernames fetched once on mount — tasks from these users are excluded from all reports
    const [guestUsernames, setGuestUsernames] = useState<Set<string>>(new Set());
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [lastLoaded, setLastLoaded] = useState<string | null>(null);

    // Load dropdowns and GUEST usernames once on mount
    useEffect(() => {
        reportsApi
            .fetchDropdownValues()
            .then(setDropdowns)
            .catch(() => {
                // Non-fatal: billability defaults to billable when no dropdown data
                console.error(
                    "Could not load dropdown values for billability checks.",
                );
            });

        // Fetch GUEST-role usernames so they can be excluded from all analytics reports
        userManagementApi
            .fetchUsers({ role: "GUEST" })
            .then((users) => setGuestUsernames(new Set(users.map((u) => u.username))))
            .catch(() => {
                console.error("Could not load GUEST users for analytics filtering.");
            });
    }, []);

    const loadReports = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const rawFetchedTasks = await reportsApi.fetchTaskActivities(
                startDate || undefined,
                endDate || undefined,
            );

            // Exclude GUEST-role users and tasks with inactive clients/projects
            const tasks = filterAnalyticsTasks(rawFetchedTasks, guestUsernames, dropdowns);

            // Cache filtered tasks so stale threshold slider can recompute without re-fetching
            setRawTasks(tasks);

            // Original two reports
            setUserSummaries(computeUserSummaries(tasks, dropdowns));
            setUserHours(computeUserHours(tasks, dropdowns));

            // New reports
            setPhaseDistribution(computePhaseDistribution(tasks));
            setStaleProjects(computeStaleProjects(tasks, staleDays));
            setClientBillability(computeClientBillability(tasks, dropdowns));
            setClientTimeline(computeClientTimeline(tasks));
            setDayOfWeekHours(
                computeDayOfWeekHours(
                    tasks,
                    startDate || undefined,
                    endDate || undefined,
                ),
            );
            setTrackingCompliance(
                computeTrackingCompliance(tasks, startDate, endDate),
            );
            setTaskRepetition(computeTaskRepetition(tasks));

            // Period Delta — requires prior period fetch; apply same filters to prior period
            const prior = getPriorPeriodDates(startDate, endDate);
            if (prior) {
                const rawPriorTasks = await reportsApi.fetchTaskActivities(
                    prior.priorStart,
                    prior.priorEnd,
                );
                const priorTasks = filterAnalyticsTasks(rawPriorTasks, guestUsernames, dropdowns);
                setPeriodDelta(
                    computePeriodDelta(
                        tasks,
                        priorTasks,
                        prior.currentLabel,
                        prior.label,
                    ),
                );
            } else {
                setPeriodDelta(null);
            }

            setLastLoaded(new Date().toLocaleTimeString());
        } catch (err) {
            console.error("Failed to load analytics data:", err);
            setError("Failed to load analytics data. Please try again.");
        } finally {
            setLoading(false);
        }
    }, [startDate, endDate, dropdowns, guestUsernames, staleDays]);

    // Auto-load when dropdowns are ready (or on initial mount with empty dropdowns)
    useEffect(() => {
        loadReports();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [dropdowns]); // reload once dropdowns arrive so billability is accurate

    // Re-run when a preset/date changes
    useEffect(() => {
        loadReports();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [startDate, endDate]);

    /** Recompute stale projects when slider changes (no network request). */
    const handleStaleDaysChange = useCallback(
        (days: number) => {
            setStaleDays(days);
            setStaleProjects(computeStaleProjects(rawTasks, days));
        },
        [rawTasks],
    );

    const handlePresetClick = (preset: DateRangePreset) => {
        setActivePreset(preset);
        const range = getDateRangeForPreset(preset);
        setStartDate(range.startDate ?? "");
        setEndDate(range.endDate ?? "");
    };

    const handleApplyCustomRange = () => {
        setActivePreset("allTime"); // deselect preset buttons when using custom range
        loadReports();
    };

    const handleClearCustomRange = () => {
        handlePresetClick("currentMonth");
    };

    const hasDateRange =
        activePreset !== "allTime" && Boolean(startDate && endDate);

    return (
        <Box>
            {/* Page Header */}
            <Box
                sx={{ display: "flex", alignItems: "center", mb: 3, gap: 1.5 }}
            >
                <AnalyticsIcon sx={{ fontSize: 32, color: "primary.main" }} />
                <Box>
                    <Typography variant="h5" fontWeight={700}>
                        Analytics & Reports
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        User activity analysis and performance reporting
                    </Typography>
                </Box>

                {lastLoaded && (
                    <Chip
                        label={`Updated ${lastLoaded}`}
                        size="small"
                        variant="outlined"
                        sx={{ ml: "auto" }}
                    />
                )}

                <Button
                    variant="outlined"
                    startIcon={<RefreshIcon />}
                    onClick={loadReports}
                    disabled={loading}
                    size="small"
                    sx={{ ml: lastLoaded ? 0 : "auto" }}
                >
                    Refresh
                </Button>
            </Box>

            {/* Date Range Filters */}
            <Paper variant="outlined" sx={{ p: 2, mb: 3 }}>
                <Typography
                    variant="subtitle2"
                    sx={{ mb: 1.5, fontWeight: 600 }}
                >
                    Date Range
                </Typography>

                {/* Preset buttons */}
                <ButtonGroup
                    variant="outlined"
                    size="small"
                    sx={{ mb: 2, flexWrap: "wrap", gap: 0.5 }}
                >
                    {DATE_RANGE_PRESETS.map(({ preset, label }) => (
                        <Button
                            key={preset}
                            onClick={() => handlePresetClick(preset)}
                            variant={
                                activePreset === preset
                                    ? "contained"
                                    : "outlined"
                            }
                        >
                            {label}
                        </Button>
                    ))}
                </ButtonGroup>

                <Divider sx={{ my: 1.5 }} />

                {/* Custom date range */}
                <Box
                    sx={{
                        display: "flex",
                        gap: 2,
                        alignItems: "center",
                        flexWrap: "wrap",
                    }}
                >
                    <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{ mr: 1 }}
                    >
                        Custom:
                    </Typography>
                    <TextField
                        type="date"
                        label="Start Date"
                        size="small"
                        value={startDate}
                        onChange={(e) => setStartDate(e.target.value)}
                        slotProps={{ inputLabel: { shrink: true } }}
                        sx={{ width: 180 }}
                    />
                    <TextField
                        type="date"
                        label="End Date"
                        size="small"
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                        slotProps={{ inputLabel: { shrink: true } }}
                        sx={{ width: 180 }}
                    />
                    <Button
                        variant="contained"
                        size="small"
                        onClick={handleApplyCustomRange}
                        disabled={loading}
                    >
                        Apply
                    </Button>
                    <Button
                        variant="outlined"
                        color="error"
                        size="small"
                        onClick={handleClearCustomRange}
                    >
                        Clear
                    </Button>
                </Box>
            </Paper>

            {/* Error state */}
            {error && (
                <Alert
                    severity="error"
                    sx={{ mb: 2 }}
                    onClose={() => setError(null)}
                >
                    {error}
                </Alert>
            )}

            {/* Summary stats bar */}
            {!loading && userSummaries.length > 0 && (
                <Box
                    sx={{
                        display: "flex",
                        gap: 3,
                        mb: 3,
                        p: 2,
                        bgcolor: "background.paper",
                        borderRadius: 1,
                        border: "1px solid",
                        borderColor: "divider",
                        flexWrap: "wrap",
                    }}
                >
                    <Box>
                        <Typography variant="caption" color="text.secondary">
                            Active Users
                        </Typography>
                        <Typography variant="h6" fontWeight={700}>
                            {userSummaries.length}
                        </Typography>
                    </Box>
                    <Divider orientation="vertical" flexItem />
                    <Box>
                        <Typography variant="caption" color="text.secondary">
                            Total Hours
                        </Typography>
                        <Typography variant="h6" fontWeight={700}>
                            {userSummaries
                                .reduce((s, u) => s + u.totalHours, 0)
                                .toFixed(1)}
                            h
                        </Typography>
                    </Box>
                    <Divider orientation="vertical" flexItem />
                    <Box>
                        <Typography variant="caption" color="text.secondary">
                            Total Billable
                        </Typography>
                        <Typography
                            variant="h6"
                            fontWeight={700}
                            color="success.main"
                        >
                            {userSummaries
                                .reduce((s, u) => s + u.billableHours, 0)
                                .toFixed(1)}
                            h
                        </Typography>
                    </Box>
                    <Divider orientation="vertical" flexItem />
                    <Box>
                        <Typography variant="caption" color="text.secondary">
                            Total Non-Billable
                        </Typography>
                        <Typography
                            variant="h6"
                            fontWeight={700}
                            color="error.main"
                        >
                            {userSummaries
                                .reduce((s, u) => s + u.nonBillableHours, 0)
                                .toFixed(1)}
                            h
                        </Typography>
                    </Box>
                    <Divider orientation="vertical" flexItem />
                    <Box>
                        <Typography variant="caption" color="text.secondary">
                            Overall Billability
                        </Typography>
                        <Typography variant="h6" fontWeight={700}>
                            {(() => {
                                const total = userSummaries.reduce(
                                    (s, u) => s + u.totalHours,
                                    0,
                                );
                                const bill = userSummaries.reduce(
                                    (s, u) => s + u.billableHours,
                                    0,
                                );
                                return total > 0
                                    ? `${((bill / total) * 100).toFixed(1)}%`
                                    : "—";
                            })()}
                        </Typography>
                    </Box>
                </Box>
            )}

            {/* Loading state */}
            {loading && (
                <Box sx={{ display: "flex", justifyContent: "center", py: 6 }}>
                    <CircularProgress />
                </Box>
            )}

            {/* Tabs */}
            {!loading && (
                <Box>
                    <Tabs
                        value={activeTab}
                        onChange={(_, v) => setActiveTab(v)}
                        variant="scrollable"
                        scrollButtons="auto"
                        sx={{ borderBottom: 1, borderColor: "divider", mb: 1 }}
                    >
                        <Tab icon={<LeaderboardIcon fontSize="small" />} iconPosition="start" label="User Summary" />
                        <Tab icon={<BarChartIcon fontSize="small" />} iconPosition="start" label="Hours by User" />
                        <Tab icon={<AccountTreeIcon fontSize="small" />} iconPosition="start" label="Phase Distribution" />
                        <Tab icon={<HourglassEmptyIcon fontSize="small" />} iconPosition="start" label="Stale Projects" />
                        <Tab icon={<PieChartIcon fontSize="small" />} iconPosition="start" label="Client Billability" />
                        <Tab icon={<TimelineIcon fontSize="small" />} iconPosition="start" label="Client Timeline" />
                        <Tab icon={<CalendarMonthIcon fontSize="small" />} iconPosition="start" label="Day of Week" />
                        <Tab icon={<FactCheckIcon fontSize="small" />} iconPosition="start" label="Tracking Compliance" />
                        <Tab icon={<RepeatIcon fontSize="small" />} iconPosition="start" label="Task Repetition" />
                        <Tab icon={<CompareArrowsIcon fontSize="small" />} iconPosition="start" label="Period Delta" />
                    </Tabs>

                    <TabPanel value={activeTab} index={0}>
                        <UserSummaryTable summaries={userSummaries} />
                    </TabPanel>

                    <TabPanel value={activeTab} index={1}>
                        <HoursByUserChart data={userHours} />
                    </TabPanel>

                    <TabPanel value={activeTab} index={2}>
                        <PhaseDistributionTable data={phaseDistribution} />
                    </TabPanel>

                    <TabPanel value={activeTab} index={3}>
                        <StaleProjectsTable
                            data={staleProjects}
                            defaultStaleDays={staleDays}
                            onStaleDaysChange={handleStaleDaysChange}
                        />
                    </TabPanel>

                    <TabPanel value={activeTab} index={4}>
                        <ClientBillabilityTable data={clientBillability} />
                    </TabPanel>

                    <TabPanel value={activeTab} index={5}>
                        <ClientTimelineTable data={clientTimeline} />
                    </TabPanel>

                    <TabPanel value={activeTab} index={6}>
                        <DayOfWeekChart data={dayOfWeekHours} />
                    </TabPanel>

                    <TabPanel value={activeTab} index={7}>
                        <TrackingComplianceTable
                            data={trackingCompliance}
                            hasDateRange={hasDateRange}
                        />
                    </TabPanel>

                    <TabPanel value={activeTab} index={8}>
                        <TaskRepetitionTable data={taskRepetition} />
                    </TabPanel>

                    <TabPanel value={activeTab} index={9}>
                        <PeriodDeltaTable
                            data={periodDelta}
                            hasPriorPeriod={hasDateRange && periodDelta !== null}
                        />
                    </TabPanel>
                </Box>
            )}
        </Box>
    );
};
