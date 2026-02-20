/**
 * Description: Dialog for managing a user's dropdown access assignments. Allows admins to
 * assign specific TASK and EXPENSE clients and projects to a user, and to toggle the
 * "All Users" flag which makes a dropdown value visible to everyone without explicit assignment.
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import React, { useState, useEffect } from "react";
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    Typography,
    Box,
    Grid,
    Checkbox,
    FormControlLabel,
    Chip,
    CircularProgress,
    Alert,
    ToggleButton,
    ToggleButtonGroup,
    Divider,
    Tooltip,
} from "@mui/material";
import PublicIcon from "@mui/icons-material/Public";
import LockIcon from "@mui/icons-material/Lock";
import type { User } from "../../types/userManagement.types";
import type {
    UserAccessData,
    UserAccessUpdateRequest,
} from "../../types/userManagement.types";
import { userManagementApi } from "../../api/userManagement.api";
import { dropdownApi } from "../../api/dropdown.api";
import type { DropdownValue } from "../../types/dropdown.types";

interface UserAccessDialogProps {
    open: boolean;
    onClose: () => void;
    user: User | null;
}

type ViewTab = "TASK" | "EXPENSE";

export const UserAccessDialog: React.FC<UserAccessDialogProps> = ({
    open,
    onClose,
    user,
}) => {
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [view, setView] = useState<ViewTab>("TASK");

    // Full dropdown item lists (including allUsers flag)
    const [allClients, setAllClients] = useState<DropdownValue[]>([]);
    const [allProjects, setAllProjects] = useState<DropdownValue[]>([]);
    const [allExpenseClients, setAllExpenseClients] = useState<DropdownValue[]>(
        [],
    );
    const [allExpenseProjects, setAllExpenseProjects] = useState<
        DropdownValue[]
    >([]);

    // Selected IDs per group (tracks admin's checkbox choices)
    const [selectedClientIds, setSelectedClientIds] = useState<Set<number>>(
        new Set(),
    );
    const [selectedProjectIds, setSelectedProjectIds] = useState<Set<number>>(
        new Set(),
    );
    const [selectedExpenseClientIds, setSelectedExpenseClientIds] = useState<
        Set<number>
    >(new Set());
    const [selectedExpenseProjectIds, setSelectedExpenseProjectIds] = useState<
        Set<number>
    >(new Set());

    useEffect(() => {
        if (open && user) {
            setView("TASK");
            loadAccessData();
        }
    }, [open, user]);

    const loadAccessData = async () => {
        if (!user) return;
        setLoading(true);
        setError(null);
        try {
            const data: UserAccessData =
                await userManagementApi.fetchUserAccess(user.username);
            setAllClients(data.allClients);
            setAllProjects(data.allProjects);
            setAllExpenseClients(data.allExpenseClients);
            setAllExpenseProjects(data.allExpenseProjects);

            const assigned = new Set<number>(data.assignedIds);
            setSelectedClientIds(
                new Set(
                    data.allClients
                        .filter((i) => assigned.has(i.id))
                        .map((i) => i.id),
                ),
            );
            setSelectedProjectIds(
                new Set(
                    data.allProjects
                        .filter((i) => assigned.has(i.id))
                        .map((i) => i.id),
                ),
            );
            setSelectedExpenseClientIds(
                new Set(
                    data.allExpenseClients
                        .filter((i) => assigned.has(i.id))
                        .map((i) => i.id),
                ),
            );
            setSelectedExpenseProjectIds(
                new Set(
                    data.allExpenseProjects
                        .filter((i) => assigned.has(i.id))
                        .map((i) => i.id),
                ),
            );
        } catch {
            setError("Failed to load access data. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    /**
     * Toggle the allUsers flag on an item and update the local list state.
     */
    const handleToggleAllUsers = async (
        item: DropdownValue,
        listSetter: React.Dispatch<React.SetStateAction<DropdownValue[]>>,
    ) => {
        try {
            const updated = await dropdownApi.toggleAllUsers(item.id);
            listSetter((prev) =>
                prev.map((i) => (i.id === updated.id ? updated : i)),
            );
        } catch {
            setError("Failed to toggle All Users flag.");
        }
    };

    const toggleId = (
        id: number,
        setter: React.Dispatch<React.SetStateAction<Set<number>>>,
    ) => {
        setter((prev) => {
            const next = new Set(prev);
            if (next.has(id)) {
                next.delete(id);
            } else {
                next.add(id);
            }
            return next;
        });
    };

    const handleSave = async () => {
        if (!user) return;
        setSaving(true);
        setError(null);
        try {
            const payload: UserAccessUpdateRequest = {
                view,
                clientIds: view === "TASK" ? Array.from(selectedClientIds) : [],
                projectIds:
                    view === "TASK" ? Array.from(selectedProjectIds) : [],
                expenseClientIds:
                    view === "EXPENSE"
                        ? Array.from(selectedExpenseClientIds)
                        : [],
                expenseProjectIds:
                    view === "EXPENSE"
                        ? Array.from(selectedExpenseProjectIds)
                        : [],
            };
            await userManagementApi.saveUserAccess(user.username, payload);
            onClose();
        } catch {
            setError("Failed to save access assignments. Please try again.");
        } finally {
            setSaving(false);
        }
    };

    /**
     * Renders a group of checkboxes with allUsers toggle buttons.
     */
    const renderItemList = (
        title: string,
        items: DropdownValue[],
        selectedIds: Set<number>,
        setter: React.Dispatch<React.SetStateAction<Set<number>>>,
        listSetter: React.Dispatch<React.SetStateAction<DropdownValue[]>>,
    ) => (
        <Box>
            <Typography
                variant="subtitle2"
                sx={{ fontWeight: 600, mb: 1, color: "text.primary" }}
            >
                {title}
            </Typography>
            {items.length === 0 ? (
                <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ fontStyle: "italic" }}
                >
                    No items available
                </Typography>
            ) : (
                items.map((item) => (
                    <Box
                        key={item.id}
                        sx={{
                            display: "flex",
                            alignItems: "center",
                            gap: 0.5,
                            mb: 0.5,
                            flexWrap: "wrap",
                            p: 0.5,
                            borderRadius: 1,
                            "&:hover": { bgcolor: "action.hover" },
                        }}
                    >
                        <FormControlLabel
                            control={
                                <Checkbox
                                    size="small"
                                    checked={
                                        item.allUsers ||
                                        selectedIds.has(item.id)
                                    }
                                    disabled={item.allUsers === true}
                                    onChange={() => toggleId(item.id, setter)}
                                    sx={{ py: 0 }}
                                />
                            }
                            label={
                                <Typography
                                    variant="body2"
                                    sx={{ fontSize: "0.85rem" }}
                                >
                                    {item.itemValue}
                                </Typography>
                            }
                            sx={{ mr: 0, flexGrow: 1 }}
                        />
                        {item.allUsers ? (
                            <>
                                <Chip
                                    label="All Users"
                                    size="small"
                                    color="primary"
                                    variant="outlined"
                                    sx={{ fontSize: "0.7rem", height: 20 }}
                                />
                                <Tooltip title="Restrict to specific users">
                                    <Button
                                        size="small"
                                        startIcon={
                                            <LockIcon
                                                sx={{
                                                    fontSize:
                                                        "0.8rem !important",
                                                }}
                                            />
                                        }
                                        onClick={() =>
                                            handleToggleAllUsers(
                                                item,
                                                listSetter,
                                            )
                                        }
                                        sx={{
                                            fontSize: "0.7rem",
                                            py: 0,
                                            minWidth: 0,
                                            px: 0.5,
                                        }}
                                    >
                                        Restrict
                                    </Button>
                                </Tooltip>
                            </>
                        ) : (
                            <Tooltip title="Make visible to all users">
                                <Button
                                    size="small"
                                    color="primary"
                                    startIcon={
                                        <PublicIcon
                                            sx={{
                                                fontSize: "0.8rem !important",
                                            }}
                                        />
                                    }
                                    onClick={() =>
                                        handleToggleAllUsers(item, listSetter)
                                    }
                                    sx={{
                                        fontSize: "0.7rem",
                                        py: 0,
                                        minWidth: 0,
                                        px: 0.5,
                                    }}
                                >
                                    All
                                </Button>
                            </Tooltip>
                        )}
                    </Box>
                ))
            )}
        </Box>
    );

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>
                <Typography variant="h6">Manage Dropdown Access</Typography>
                {user && (
                    <Typography variant="subtitle2" color="text.secondary">
                        User: <strong>{user.username}</strong>
                    </Typography>
                )}
            </DialogTitle>

            <DialogContent dividers>
                {loading ? (
                    <Box
                        sx={{ display: "flex", justifyContent: "center", p: 4 }}
                    >
                        <CircularProgress />
                    </Box>
                ) : (
                    <>
                        {error && (
                            <Alert
                                severity="error"
                                sx={{ mb: 2 }}
                                onClose={() => setError(null)}
                            >
                                {error}
                            </Alert>
                        )}

                        {/* Tab toggle */}
                        <Box
                            sx={{
                                display: "flex",
                                alignItems: "center",
                                gap: 2,
                                mb: 2,
                            }}
                        >
                            <ToggleButtonGroup
                                value={view}
                                exclusive
                                onChange={(_, v) => v && setView(v)}
                                size="small"
                            >
                                <ToggleButton value="TASK">Task</ToggleButton>
                                <ToggleButton value="EXPENSE">
                                    Expense
                                </ToggleButton>
                            </ToggleButtonGroup>
                            <Typography
                                variant="caption"
                                color="text.secondary"
                            >
                                {view === "TASK"
                                    ? "Assign Task clients and projects"
                                    : "Assign Expense clients and projects"}
                                {
                                    " — Saving only updates this tab's assignments."
                                }
                            </Typography>
                        </Box>

                        <Divider sx={{ mb: 2 }} />

                        {view === "TASK" ? (
                            <Grid container spacing={3}>
                                <Grid size={{ xs: 12, sm: 6 }}>
                                    {renderItemList(
                                        "Clients (Task)",
                                        allClients,
                                        selectedClientIds,
                                        setSelectedClientIds,
                                        setAllClients,
                                    )}
                                </Grid>
                                <Grid size={{ xs: 12, sm: 6 }}>
                                    {renderItemList(
                                        "Projects (Task)",
                                        allProjects,
                                        selectedProjectIds,
                                        setSelectedProjectIds,
                                        setAllProjects,
                                    )}
                                </Grid>
                            </Grid>
                        ) : (
                            <Grid container spacing={3}>
                                <Grid size={{ xs: 12, sm: 6 }}>
                                    {renderItemList(
                                        "Clients (Expense)",
                                        allExpenseClients,
                                        selectedExpenseClientIds,
                                        setSelectedExpenseClientIds,
                                        setAllExpenseClients,
                                    )}
                                </Grid>
                                <Grid size={{ xs: 12, sm: 6 }}>
                                    {renderItemList(
                                        "Projects (Expense)",
                                        allExpenseProjects,
                                        selectedExpenseProjectIds,
                                        setSelectedExpenseProjectIds,
                                        setAllExpenseProjects,
                                    )}
                                </Grid>
                            </Grid>
                        )}
                    </>
                )}
            </DialogContent>

            <DialogActions>
                <Button onClick={onClose} color="inherit" disabled={saving}>
                    Cancel
                </Button>
                <Button
                    onClick={handleSave}
                    variant="contained"
                    disabled={saving || loading}
                >
                    {saving ? "Saving…" : "Save"}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
