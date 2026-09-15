"use client";

import Paper from "@mui/material/Paper";
import { Clock, MessageSquareQuote } from "lucide-react";

export function MessageTemplatesSection() {
  return (
    <Paper
      variant="outlined"
      className="border! border-border! rounded-2xl! p-12! flex! flex-col! items-center! text-center! bg-surface!"
    >
      <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center text-primary mb-4">
        <MessageSquareQuote size={32} />
      </div>
      <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-700 dark:text-amber-300 border border-amber-500/30 mb-3">
        <Clock size={12} />
        <span>En cours de développement</span>
      </div>
      <h3 className="text-xl font-bold text-text m-0">Templates de messages</h3>
    </Paper>
  );
}
