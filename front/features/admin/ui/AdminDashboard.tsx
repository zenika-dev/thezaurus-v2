"use client";

import { useState } from "react";
import { MessageSquareQuote, ShieldCheck, Users } from "lucide-react";
import { DataErrorBoundary } from "@/shared/ui";
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
    <div className="p-8">
      <div className="mb-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
            <ShieldCheck size={24} />
          </div>
          <h1 className="text-[34px] leading-[1.235] font-bold text-text m-0 tracking-[0.25px]">
            Administration
          </h1>
        </div>
      </div>

      <div className="flex items-center gap-2 border-b border-border mb-6">
        <button
          type="button"
          onClick={() => selectTab("roles")}
          className={`inline-flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors cursor-pointer ${
            activeTab === "roles"
              ? "border-primary text-primary"
              : "border-transparent text-text-muted hover:text-text hover:border-border"
          }`}
        >
          <Users size={16} />
          <span>Rôles utilisateurs</span>
        </button>
        <button
          type="button"
          onClick={() => selectTab("templates")}
          className={`inline-flex items-center gap-2 px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors cursor-pointer ${
            activeTab === "templates"
              ? "border-primary text-primary"
              : "border-transparent text-text-muted hover:text-text hover:border-border"
          }`}
        >
          <MessageSquareQuote size={16} />
          <span>Templates de messages</span>
        </button>
      </div>

      <DataErrorBoundary>
        {activeTab === "roles" && <UserRolesTable />}
        {activeTab === "templates" && <MessageTemplatesSection onDirtyChange={setTemplateDirty} />}
      </DataErrorBoundary>
    </div>
  );
}

export default AdminDashboard;
