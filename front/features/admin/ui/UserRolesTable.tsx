"use client";

import { useMemo, useState, type ReactNode } from "react";
import { useSession } from "next-auth/react";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Paper from "@mui/material/Paper";
import Tooltip from "@mui/material/Tooltip";
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";
import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import InputAdornment from "@mui/material/InputAdornment";
import TextField from "@mui/material/TextField";
import { Funnel, Plus, Search, X } from "lucide-react";
import { Role, type BackendUserAdminView } from "@/shared/api";
import { SpeakerChip } from "@/entities/user";
import { statusTones } from "@/shared/lib/status-tones";
import { Badge, FilterChip } from "@/shared/ui";
import { useAdminUsers } from "@/features/admin";
import { PaginationBar } from "@/shared/ui";

const ROLE_BADGES = {
  ADMIN: { label: "Admin", tone: statusTones.danger },
  DT: { label: "DT", tone: statusTones.info },
  CONSULTANT: { label: "Consultant", tone: statusTones.success },
} satisfies Record<Role, { label: string; tone: typeof statusTones[keyof typeof statusTones] }>;

const ALL_ROLES: Role[] = [...Role];

/** Static roles and removable roles share the same badge; only the action changes. */
function RoleBadge({ role, children }: { role: Role; children?: ReactNode }) {
  const { label, tone } = ROLE_BADGES[role];
  return <Badge color={tone.text} bg={tone.bg} darkColor={tone.darkText} darkBg={tone.darkBg}>{label}{children}</Badge>;
}
export function UserRolesTable() {
  const { data: session } = useSession();
  const currentUserEmail = session?.user?.email?.toLowerCase() ?? "";

  const { users, updateRoles, isUpdating } = useAdminUsers();

  const [searchQuery, setSearchQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState<"All" | Role>("All");
  const [page, setPage] = useState(1);
  const [rowsPerPage, setRowsPerPage] = useState(10);

  const [toast, setToast] = useState<{ message: string; severity: "success" | "error" } | null>(
    null,
  );

  const [menuAnchor, setMenuAnchor] = useState<{
    el: HTMLElement;
    user: BackendUserAdminView;
  } | null>(null);

  const filteredUsers = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return users.filter((user) => {
      if (q) {
        const matchName = user.name.toLowerCase().includes(q);
        const matchEmail = user.email.toLowerCase().includes(q);
        if (!matchName && !matchEmail) return false;
      }
      if (roleFilter !== "All") {
        if (!user.roles.includes(roleFilter)) return false;
      }
      return true;
    });
  }, [users, searchQuery, roleFilter]);

  const totalPages = Math.ceil(filteredUsers.length / rowsPerPage);

  const paginatedUsers = useMemo(() => {
    const start = (page - 1) * rowsPerPage;
    return filteredUsers.slice(start, start + rowsPerPage);
  }, [filteredUsers, page, rowsPerPage]);

  const handleOpenAddMenu = (event: React.MouseEvent<HTMLElement>, user: BackendUserAdminView) => {
    setMenuAnchor({ el: event.currentTarget, user });
  };

  const handleCloseAddMenu = () => {
    setMenuAnchor(null);
  };

  const handleAddRole = async (user: BackendUserAdminView, newRole: Role) => {
    handleCloseAddMenu();
    const updatedRoles = [...user.roles, newRole];
    try {
      await updateRoles({ email: user.email, roles: updatedRoles });
      setToast({ message: `Rôle ${newRole} ajouté à ${user.name}`, severity: "success" });
    } catch {
      setToast({ message: "Erreur lors de l'ajout du rôle", severity: "error" });
    }
  };

  const handleRemoveRole = async (user: BackendUserAdminView, roleToRemove: Role) => {
    if (user.roles.length <= 1) {
      setToast({ message: "Un utilisateur doit posséder au moins un rôle.", severity: "error" });
      return;
    }
    const updatedRoles = user.roles.filter((r) => r !== roleToRemove);
    try {
      await updateRoles({ email: user.email, roles: updatedRoles });
      setToast({ message: `Rôle ${roleToRemove} retiré de ${user.name}`, severity: "success" });
    } catch {
      setToast({ message: "Erreur lors de la suppression du rôle", severity: "error" });
    }
  };


  const availableRolesForMenu = useMemo(() => {
    if (!menuAnchor) return [];
    return ALL_ROLES.filter((role) => !menuAnchor.user.roles.includes(role));
  }, [menuAnchor]);

  return (
    <div className="flex flex-col flex-1">
      <div className="flex flex-col gap-4 mb-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <TextField
            size="small"
            label="Rechercher par nom ou email"
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setPage(1);
            }}
            className="w-full sm:w-80"
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <Search size={16} className="text-text-muted" />
                  </InputAdornment>
                ),

              },
            }}
          />

          <div className="flex items-center gap-2 flex-wrap">
            <Funnel size={14} className="text-text-muted shrink-0" />
            <span className="text-xs text-text-muted mr-1">Rôle :</span>
            {(["All", ...Role] as const).map((role) => (
              <FilterChip
                key={role}
                label={role === "All" ? "Tous" : role}
                active={roleFilter === role}
                onClick={() => {
                  setRoleFilter(role);
                  setPage(1);
                }}
              />
            ))}
            {roleFilter !== "All" && (
              <button
                onClick={() => {
                  setRoleFilter("All");
                  setPage(1);
                }}
                className="flex min-h-11 text-xs items-center font-sans gap-1 cursor-pointer text-link border border-primary/20 px-3 py-1 rounded-2xl bg-primary/10 transition-colors hover:bg-primary/20"
              >
                Réinitialiser
              </button>
            )}
          </div>
        </div>
      </div>

      <TableContainer
        component={Paper}
        variant="outlined"
        className="border border-primary rounded-2xl mb-4"
        sx={{ overflowX: "auto", maxWidth: "100%" }}
        tabIndex={0}
        role="region"
        aria-label="Rôles utilisateurs, tableau défilant"
      >
        <Table sx={{ minWidth: 680 }}>
          <TableHead className="bg-surface-muted">
            <TableRow>
              <TableCell>
                <strong>Utilisateur</strong>
              </TableCell>
              <TableCell>
                <strong>Email</strong>
              </TableCell>
              <TableCell>
                <strong>Rôles</strong>
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {paginatedUsers.length === 0 ? (
              <TableRow>
                <TableCell colSpan={3} align="center" className="py-8 text-text-muted">
                  Aucun utilisateur ne correspond à ces critères.
                </TableCell>
              </TableRow>
            ) : (
              paginatedUsers.map((user) => {
                const isCurrentUser = user.email.toLowerCase() === currentUserEmail;
                const unassignedRoles = ALL_ROLES.filter((role) => !user.roles.includes(role));

                return (
                  <TableRow key={user.email} hover>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <SpeakerChip
                          name={user.name}
                          email={user.email}
                          size="small"
                          className="text-xs h-7"
                        />
                        {isCurrentUser && (
                          <span className="text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded-full bg-primary/10 text-link border border-primary/20">
                            Vous
                          </span>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="text-sm text-text-muted">{user.email}</TableCell>
                    <TableCell>
                      <div className="flex items-center flex-wrap gap-1.5">
                        {user.roles.map((role) => {
                          const config = ROLE_BADGES[role];
                          const canRemove = !isCurrentUser && user.roles.length > 1;

                          return (
                            <RoleBadge key={role} role={role}>
                              {canRemove && (
                                <Tooltip title={`Retirer le rôle ${config.label}`} arrow>
                                  <button
                                    type="button"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      handleRemoveRole(user, role);
                                    }}
                                    disabled={isUpdating}
                                    className="inline-flex items-center justify-center min-w-11 min-h-11 rounded-full text-inherit opacity-60 hover:opacity-100 hover:bg-black/10 dark:hover:bg-white/20 transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed border-0 p-0 bg-transparent"
                                    aria-label={`Retirer le rôle ${config.label} de ${user.name}`}
                                  >
                                    <X size={16} strokeWidth={2.5} />
                                  </button>
                                </Tooltip>
                              )}
                            </RoleBadge>
                          );
                        })}

                        {isCurrentUser ? (
                          <Tooltip title="Vous ne pouvez pas modifier vos propres rôles" arrow>
                            <span className="inline-flex items-center gap-1 pl-2 pr-2.5 py-0.5 rounded-full text-xs font-medium border border-dashed border-border text-text-muted opacity-40 cursor-not-allowed">
                              <Plus size={12} strokeWidth={2.5} />
                              <span>Rôle</span>
                            </span>
                          </Tooltip>
                        ) : unassignedRoles.length > 0 ? (
                          <Tooltip title="Ajouter un rôle" arrow>
                            <button
                              type="button"
                              onClick={(e) => handleOpenAddMenu(e, user)}
                              aria-label={`Ajouter un rôle à ${user.name}`}
                              aria-haspopup="menu"
                              aria-expanded={menuAnchor?.user.email === user.email}
                              disabled={isUpdating}
                              className="inline-flex min-h-11 items-center gap-1 pl-2 pr-2.5 py-0.5 rounded-full text-xs font-medium border border-dashed border-border disabled:opacity-40 disabled:cursor-not-allowed hover:border-primary hover:text-link hover:bg-primary/5 text-text-muted transition-colors cursor-pointer bg-transparent"
                            >
                              <Plus size={12} strokeWidth={2.5} />
                              <span>Rôle</span>
                            </button>
                          </Tooltip>
                        ) : null}
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <PaginationBar
        page={page}
        pageSize={rowsPerPage}
        total={filteredUsers.length}
        totalPages={totalPages}
        onPageChange={setPage}
        onPageSizeChange={(newSize) => {
          setRowsPerPage(newSize);
          setPage(1);
        }}
        pageSizeOptions={[10, 25, 50]}
        label="Utilisateurs par page :"
        ariaLabel="Pagination des utilisateurs"
      />

      <Menu
        anchorEl={menuAnchor?.el}
        open={Boolean(menuAnchor)}
        onClose={handleCloseAddMenu}
        slotProps={{
          paper: {
            sx: { border: 1, borderColor: "divider", borderRadius: "12px" },
          },
        }}
      >
        {availableRolesForMenu.map((role) => (
          <MenuItem
            key={role}
            onClick={() => menuAnchor && handleAddRole(menuAnchor.user, role)}
            className="text-sm font-semibold gap-2"
            sx={{ minHeight: 44 }}
          >
            <RoleBadge role={role} />
            <span className="text-xs text-text-muted">
              {role === "ADMIN"
                ? "Accès complet"
                : role === "DT"
                  ? "Directeur Technique"
                  : "Consultant / Speaker"}
            </span>
          </MenuItem>
        ))}
      </Menu>

      <Snackbar
        open={!!toast}
        autoHideDuration={4000}
        onClose={() => setToast(null)}
        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
      >
        {toast ? (
          <Alert
            onClose={() => setToast(null)}
            severity={toast.severity}
            className="shadow-md rounded-xl text-xs flex items-center"
          >
            {toast.message}
          </Alert>
        ) : undefined}
      </Snackbar>
    </div>
  );
}
