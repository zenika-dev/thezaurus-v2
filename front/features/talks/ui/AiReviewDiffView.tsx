"use client";

import TextField from "@mui/material/TextField";
import Button from "@mui/material/Button";
import Tooltip from "@mui/material/Tooltip";
import Chip from "@mui/material/Chip";
import { Sparkles, Edit3, ArrowLeft, Plus } from "lucide-react";

interface AiReviewDiffViewProps {
  label: string;
  value: string;
  onChange: (newValue: string) => void;
  suggestions: string[];
  multiline?: boolean;
  rows?: number;
  placeholder?: string;
}

export function AiReviewDiffView({
  label,
  value,
  onChange,
  suggestions,
  multiline = false,
  rows = 4,
  placeholder = "",
}: AiReviewDiffViewProps) {
  const handleSelectSuggestion = (sug: string) => {
    onChange(sug);
  };

  const handleAppendSuggestion = (sug: string) => {
    if (!value.trim()) {
      onChange(sug);
    } else {
      onChange(`${value.trim()}\n\n${sug}`);
    }
  };

  return (
    <div className="flex flex-col gap-2.5">
      <div className="flex items-center justify-between">
        <label className="text-sm font-semibold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
          <Edit3 size={16} className="text-purple-600 dark:text-purple-400" />
          {label}
        </label>
        <span className="text-xs text-text-muted">
          {value.length} caractères
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Left panel: Editable original content */}
        <div className="border border-purple-200 dark:border-purple-900/60 bg-purple-50/20 dark:bg-purple-950/20 rounded-xl p-3.5 flex flex-col gap-2">
          <div className="flex justify-between items-center pb-1 border-b border-purple-200 dark:border-purple-900/40">
            <span className="text-xs font-bold text-purple-700 dark:text-purple-300 uppercase tracking-wider flex items-center gap-1">
              <Edit3 size={12} />
              Contenu original (Modifiable)
            </span>
            <Chip
              label="Modifiable"
              size="small"
              className="bg-purple-100 dark:bg-purple-900/60 text-purple-800 dark:text-purple-200 text-[10px] h-5 font-semibold"
            />
          </div>

          <TextField
            value={value}
            onChange={(e) => onChange(e.target.value)}
            multiline={multiline}
            rows={multiline ? rows : undefined}
            fullWidth
            placeholder={placeholder}
            variant="outlined"
            size="small"
            slotProps={{
              input: {
                className: "bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 text-sm leading-relaxed font-sans",
              },
            }}
          />
        </div>

        {/* Right panel: List of AI Suggestions */}
        <div className="border border-emerald-200 dark:border-emerald-900/60 bg-emerald-50/20 dark:bg-emerald-950/20 rounded-xl p-3.5 flex flex-col gap-2">
          <div className="flex justify-between items-center pb-1 border-b border-emerald-200 dark:border-emerald-900/40">
            <span className="text-xs font-bold text-emerald-700 dark:text-emerald-400 uppercase tracking-wider flex items-center gap-1">
              <Sparkles size={12} />
              Suggestions de l&apos;Assistant ({suggestions.length})
            </span>
          </div>

          <div className="flex flex-col gap-2.5 max-h-[220px] overflow-y-auto pr-1">
            {suggestions && suggestions.length > 0 ? (
              suggestions.map((sug, idx) => (
                <div
                  key={`sug-${idx}`}
                  className="bg-white dark:bg-slate-800/90 border border-slate-200 dark:border-slate-700/80 rounded-lg p-2.5 text-xs text-slate-800 dark:text-slate-200 flex flex-col gap-2 shadow-2xs hover:border-emerald-400 transition-colors"
                >
                  <p className="whitespace-pre-wrap leading-relaxed">
                    {sug}
                  </p>
                  <div className="flex items-center justify-end gap-1.5 pt-1 border-t border-slate-100 dark:border-slate-700/50">
                    <Tooltip
                        title={
                          multiline
                              ? "Ajouter cette suggestion à la fin de votre contenu"
                              : "Remplacer votre contenu par cette suggestion"
                        }
                    >
                      <Button size="small" variant={multiline ? "text" : "contained"}
                          onClick={() => multiline ? handleAppendSuggestion(sug) : handleSelectSuggestion(sug)}
                          startIcon={multiline ? <Plus size={12} /> : <ArrowLeft size={12} />}
                          className={
                            multiline ? "text-[11px] normal-case text-emerald-700 dark:text-emerald-300 hover:bg-emerald-50 dark:hover:bg-emerald-950/50 py-0.5 px-2 min-w-0"
                                : "text-[11px] normal-case bg-emerald-600 hover:bg-emerald-700 text-white py-0.5 px-2.5 min-w-0 shadow-2xs"
                          }
                      >
                        {multiline ? "Ajouter à mon texte" : "Utiliser cette version"}
                      </Button>
                    </Tooltip>
                  </div>
                </div>
              ))
            ) : (
              <div className="py-6 text-center text-xs text-text-muted">
                Aucune suggestion spécifique disponible pour cette section.
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
