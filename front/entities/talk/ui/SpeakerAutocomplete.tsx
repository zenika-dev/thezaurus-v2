"use client";

import { useState, useEffect, useMemo } from "react";
import Autocomplete from "@mui/material/Autocomplete";
import TextField from "@mui/material/TextField";
import CircularProgress from "@mui/material/CircularProgress";
import { Check } from "lucide-react";
import { SpeakerChip } from "@/entities/user";
import useUsersSearchQuery from "@/entities/user";
import type { SpeakerFormData } from "../schema";

export function normalizeText(text: string): string {
    return text
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase();
}

export function areSpeakersEqual(
    firstSpeaker: { name: string; email?: string | null },
    secondSpeaker: { name: string; email?: string | null }
): boolean {
    if (
        firstSpeaker.email &&
        secondSpeaker.email &&
        firstSpeaker.email.trim().length > 0 &&
        secondSpeaker.email.trim().length > 0
    ) {
        return firstSpeaker.email.trim().toLowerCase() === secondSpeaker.email.trim().toLowerCase();
    }
    return firstSpeaker.name.trim().toLowerCase() === secondSpeaker.name.trim().toLowerCase();
}

export interface SpeakerAutocompleteProps {
    value: SpeakerFormData[];
    onChange: (speakers: SpeakerFormData[]) => void;
    id?: string;
    label?: string;
    required?: boolean;
    error?: boolean;
    helperText?: string;
    disabled?: boolean;
    placeholder?: string;
    size?: "small" | "medium";
    fullWidth?: boolean;
}

export function SpeakerAutocomplete({
                                        id,
                                        value = [],
                                        onChange,
                                        label,
                                        required = false,
                                        error,
                                        helperText,
                                        disabled = false,
                                        placeholder,
                                        size = "medium",
                                        fullWidth = true,
                                    }: SpeakerAutocompleteProps) {
    const [speakerSearchInput, setSpeakerSearchInput] = useState("");
    const [debouncedSearch, setDebouncedSearch] = useState("");

    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedSearch(speakerSearchInput.trim());
        }, 300);
        return () => clearTimeout(timer);
    }, [speakerSearchInput]);

    const { data: userResults = [], isFetching: isSearchingUsers } =
        useUsersSearchQuery(debouncedSearch);

    const displayedSpeakerOptions = useMemo(() => {
        const combined: SpeakerFormData[] = [...value];
        for (const user of userResults) {
            if (user.name) {
                const candidateName = user.name;
                if (!combined.some((item) => areSpeakersEqual(item, { name: candidateName, email: user.email }))) {
                    combined.push({ name: candidateName, email: user.email ?? "" });
                }
            }
        }
        return combined;
    }, [value, userResults]);

    const handleSpeakerChange = (_changeEvent: unknown, newValue: (string | SpeakerFormData)[]) => {
        const formattedList: SpeakerFormData[] = [];
        for (const item of newValue) {
            const speakerObj: SpeakerFormData =
                typeof item === "string"
                    ? { name: item.trim(), email: "" }
                    : { name: item.name.trim(), email: item.email?.trim() || "" };

            if (
                speakerObj.name &&
                !formattedList.some((existing) => areSpeakersEqual(existing, speakerObj))
            ) {
                formattedList.push(speakerObj);
            }
        }
        setSpeakerSearchInput("");
        setDebouncedSearch("");
        onChange(formattedList);
    };

    const dynamicPlaceholder =
        value.length === 0
            ? (placeholder ?? "Rechercher ou saisir un speaker...")
            : "Ajouter un autre speaker...";

    return (
        <Autocomplete<SpeakerFormData, true, false, true>
            id={id}
            multiple
            freeSolo
            clearOnBlur
            fullWidth={fullWidth}
            size={size}
            disabled={disabled}
            options={displayedSpeakerOptions}
            getOptionKey={(option) =>
                typeof option === "string"
                    ? option
                    : option.email && option.email.trim().length > 0
                      ? option.email
                      : option.name
            }
            getOptionLabel={(option) => (typeof option === "string" ? option : option.name)}
            isOptionEqualToValue={(option, selectedOption) => {
                const first = typeof option === "string" ? { name: option, email: "" } : option;
                const second = typeof selectedOption === "string" ? { name: selectedOption, email: "" } : selectedOption;
                return areSpeakersEqual(first, second);
            }}
            filterOptions={(options, params) => {
                const normalizedInput = normalizeText(params.inputValue.trim());
                const filtered = options.filter(
                    (candidateOption) =>
                        normalizeText(candidateOption.name).includes(normalizedInput) ||
                        (candidateOption.email && normalizeText(candidateOption.email).includes(normalizedInput))
                );
                const isExisting = options.some(
                    (candidateOption) =>
                        normalizeText(candidateOption.name) === normalizedInput ||
                        (candidateOption.email && normalizeText(candidateOption.email) === normalizedInput)
                );
                if (params.inputValue.trim() !== "" && !isExisting) {
                    filtered.push({
                        name: params.inputValue.trim(),
                        email: "",
                    });
                }
                return filtered;
            }}
            value={value}
            inputValue={speakerSearchInput}
            onInputChange={(_inputChangeEvent, newInputValue, reason) => {
                if (reason === "input") {
                    setSpeakerSearchInput(newInputValue);
                } else if (reason === "clear" || reason === "reset") {
                    setSpeakerSearchInput("");
                    setDebouncedSearch("");
                }
            }}
            onChange={handleSpeakerChange}
            renderValue={(selectedValues, getItemProps) =>
                selectedValues.map((item, index) => {
                    const { key, ...itemProps } = getItemProps({ index });
                    const name = typeof item === "string" ? item : item.name;
                    const email = typeof item === "string" ? "" : item.email;
                    return (
                        <SpeakerChip
                            key={key}
                            name={name}
                            email={email}
                            size={size === "small" ? "small" : "medium"}
                            onDelete={itemProps.onDelete}
                        />
                    );
                })
            }
            slotProps={{
                paper: {
                    sx: {
                        "& .MuiAutocomplete-listbox": {
                            padding: "4px 0",
                            "& .MuiAutocomplete-option": {
                                "&.Mui-focused, &[data-focus='true']": {
                                    backgroundColor: "rgb(226 232 240) !important",
                                },
                                "&:hover": {
                                    backgroundColor: "rgb(226 232 240) !important",
                                },
                                "&[aria-selected='true']": {
                                    backgroundColor: "rgba(237, 33, 60, 0.08) !important",
                                    "&.Mui-focused, &[data-focus='true'], &:hover": {
                                        backgroundColor: "rgba(237, 33, 60, 0.16) !important",
                                    },
                                },
                            },
                        },
                    },
                },
            }}
            renderOption={(props, option, state) => {
                const { key, className: optionClassName, ...otherProps } = props;
                const name = typeof option === "string" ? option : option.name;
                const email = typeof option === "string" ? "" : option.email;
                const isInternal = Boolean(email && email.trim().length > 0);
                const isSelected = state.selected;

                return (
                    <li
                        key={key}
                        {...otherProps}
                        className={`MuiAutocomplete-option ${optionClassName || ""} flex items-center justify-between gap-2 px-3 py-2.5 cursor-pointer transition-colors ${
                            isSelected
                                ? "bg-red-50/90 dark:bg-red-950/50 text-red-950 dark:text-red-100 font-medium border-l-4 border-[#ED213C] [&.Mui-focused]:!bg-red-100 dark:[&.Mui-focused]:!bg-red-900/60 hover:!bg-red-100 dark:hover:!bg-red-900/60"
                                : "text-slate-800 dark:text-slate-200 [&.Mui-focused]:!bg-slate-200 dark:[&.Mui-focused]:!bg-slate-700 hover:!bg-slate-200 dark:hover:!bg-slate-700 data-[focus=true]:!bg-slate-200 dark:data-[focus=true]:!bg-slate-700"
                        }`}
                    >
                        <div className="flex items-center gap-2 min-w-0">
                            <SpeakerChip name={name} email={email} size="small" />
                            <span className="text-xs text-text-muted truncate">
                                {isInternal ? `(${email})` : "— Intervenant externe (texte libre)"}
                            </span>
                        </div>
                        {isSelected ? (
                            <Check className="size-4 text-[#ED213C] shrink-0 ml-2" />
                        ) : null}
                    </li>
                );
            }}
            noOptionsText={
                speakerSearchInput.trim().length === 0
                    ? "Tapez un nom pour chercher un consultant, ou Entrée pour un nom libre."
                    : "Aucun utilisateur trouvé (appuyez sur Entrée pour valider ce nom)"
            }
            renderInput={(params) => (
                <TextField
                    {...params}
                    label={label}
                    required={required}
                    fullWidth={fullWidth}
                    placeholder={dynamicPlaceholder}
                    error={error}
                    helperText={helperText}
                    slotProps={{
                        ...params.slotProps,
                        input: {
                            ...params.slotProps?.input,
                            endAdornment: (
                                <>
                                    {isSearchingUsers ? (
                                        <CircularProgress color="inherit" size={16} />
                                    ) : null}
                                    {params.slotProps?.input?.endAdornment}
                                </>
                            ),
                        },
                    }}
                />
            )}
        />
    );
}
