"use client";

import { useState } from "react";
import { Tab, Tabs } from "@mui/material";
import { MessageSquareQuote, Users } from "lucide-react";
import { DataErrorBoundary, PageHeader } from "@/shared/ui";
import { UserRolesTable } from "./UserRolesTable";
import { MessageTemplatesSection } from "@/features/admin";
import { unsavedTemplateMessage } from "../model/useUnsavedTemplate";

type AdminTab = "roles" | "templates";

export function AdminDashboard() {
  const [activeTab, setActiveTab] = useState<AdminTab>("roles");
  const [templateDirty, setTemplateDirty] = useState(false);

  function selectTab(tab: AdminTab) {
    if (tab === activeTab) return;
    if (templateDirty && !window.confirm(unsavedTemplateMessage)) return;
    setTemplateDirty(false);
    setActiveTab(tab);
  }

  return (
    <div className="p-4 sm:p-6 lg:p-8 flex flex-col min-h-full">
      <PageHeader title="Administration" />
      <Tabs value={activeTab} onChange={(_, value: AdminTab) => selectTab(value)} variant="scrollable" scrollButtons="auto" aria-label="Administration" sx={{ mb: 3, borderBottom: 1, borderColor: "divider" }}>
        <Tab value="roles" id="admin-tab-roles" aria-controls="admin-panel-roles" icon={<Users size={16} />} iconPosition="start" label="Rôles utilisateurs" />
        <Tab value="templates" id="admin-tab-templates" aria-controls="admin-panel-templates" icon={<MessageSquareQuote size={16} />} iconPosition="start" label="Templates de messages" />
      </Tabs>
      <div role="tabpanel" id={`admin-panel-${activeTab}`} aria-labelledby={`admin-tab-${activeTab}`}>
        <DataErrorBoundary>
          {activeTab === "roles" ? <UserRolesTable /> : <MessageTemplatesSection onDirtyChange={setTemplateDirty} />}
        </DataErrorBoundary>
      </div>
    </div>
  );
}
export default AdminDashboard;
