"use client";

import { useState, useEffect } from "react";
import { useTheme } from "next-themes";
import { DatePickerProvider } from "@/shared/ui";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import type { ConferenceDate } from "@/entities/conference";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import Select from "@mui/material/Select";
import MenuItem from "@mui/material/MenuItem";
import InputAdornment from "@mui/material/InputAdornment";
import IconButton from "@mui/material/IconButton";
import Box from "@mui/material/Box";
import Autocomplete from "@mui/material/Autocomplete";
import CircularProgress from "@mui/material/CircularProgress";
import { DatePicker } from "@mui/x-date-pickers";
import { ExternalLinkIcon, Library } from "lucide-react";
import dayjs, { type Dayjs } from "dayjs";
import "dayjs/locale/fr";
import { isValidUrl } from "@/shared/lib";
import {
  conferenceCFPStatusConfig,
  ConferenceData,
} from "@/entities/conference";
import {
  ConferenceFormData,
  conferenceFormSchema,
} from "@/entities/conference/schema";
import { parseLocation } from "@/entities/conference/location-utils";

dayjs.locale("fr");

const DIALOG_TITLE_ID = "create-conference-dialog-title";

interface CreateConferenceDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (conference: ConferenceData) => void;
}

interface LocationOption {
  display_name: string;
  address: Record<string, string>;
}

export function CreateConferenceDialog({
  open,
  onClose,
  onSubmit,
}: CreateConferenceDialogProps) {
  const [dateType, setDateType] = useState<ConferenceDate["type"]>("single");
  const [date, setDate] = useState<Dayjs | null>(null);
  const [dateEnd, setDateEnd] = useState<Dayjs | null>(null);
  const [dateMonth, setDateMonth] = useState<Dayjs | null>(null);
  const [dateError, setDateError] = useState<string | null>(null);
  const [cfpDateError, setCfpDateError] = useState<string | null>(null);
  const [cfpClosingDate, setCfpClosingDate] = useState<Dayjs | null>(null);

  const [openLocation, setOpenLocation] = useState(false);
  const [locationOptions, setLocationOptions] = useState<
    readonly LocationOption[]
  >([]);
  const [loadingLocation, setLoadingLocation] = useState(false);
  const [locationInputValue, setLocationInputValue] = useState("");
  const [selectedLocation, setSelectedLocation] =
    useState<LocationOption | null>(null);

  useEffect(() => {
    let active = true;

    if (locationInputValue === "") {
      setLocationOptions(locationInputValue ? [locationInputValue] : []);
      return undefined;
    }

    setLoadingLocation(true);

    const timer = setTimeout(async () => {
      try {
        const response = await fetch(
          `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(
            locationInputValue,
          )}&addressdetails=1&limit=5`,
          {
            headers: {
              "Accept-Language": "fr",
            },
          },
        );
        const data = await response.json();
        if (active) {
          setLocationOptions(
            data.map((d: LocationOption) => ({
              display_name: d.display_name,
              address: d.address,
            })),
          );
        }
      } catch (e) {
        console.error("Error fetching location data", e);
      } finally {
        if (active) {
          setLoadingLocation(false);
        }
      }
    }, 1000);

    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [locationInputValue]);

  const {
    register,
    handleSubmit,
    control,
    watch,
    reset,
    formState: { errors },
  } = useForm<ConferenceFormData>({
    resolver: zodResolver(conferenceFormSchema),
    defaultValues: {
      title: "",
      location: "",
      cfpLink: "",
      cfpStatus: "None",
      submittedTalksAmount: 0,
    },
  });

  // eslint-disable-next-line react-hooks/incompatible-library
  const cfpLink = watch("cfpLink");
  const currentStatus = watch("cfpStatus");
  const { resolvedTheme } = useTheme();
  const isDark = resolvedTheme === "dark";
  const { text: statusColor, darkText: statusDarkColor } =
    conferenceCFPStatusConfig[currentStatus];
  const appliedStatusColor = isDark ? statusDarkColor : statusColor;

  const handleClose = () => {
    reset();
    setDateType("single");
    setDate(null);
    setDateEnd(null);
    setDateError(null);
    setCfpDateError(null);
    setCfpClosingDate(null);
    onClose();
  };

  const validateDates = (): boolean => {
    let isValid = true;

    if (dateType === "single" && (!date || !date.isValid())) {
      setDateError("La date est requise et doit être valide");
      isValid = false;
    } else if (
      dateType === "range" &&
      (!date || !dateEnd || !date.isValid() || !dateEnd.isValid())
    ) {
      setDateError(
        "Les dates de début et fin sont requises et doivent être valides",
      );
      isValid = false;
    } else if (dateType === "month" && (!dateMonth || !dateMonth.isValid())) {
      setDateError("Le mois est requis et doit être valide");
      isValid = false;
    } else {
      setDateError(null);
    }

    if (cfpClosingDate && cfpClosingDate.isValid()) {
      let confDateForComparison: Dayjs | null = null;
      if (dateType === "month" && dateMonth && dateMonth.isValid()) {
        confDateForComparison = dateMonth.startOf("month");
      } else if (
        (dateType === "single" || dateType === "range") &&
        date &&
        date.isValid()
      ) {
        confDateForComparison = date.startOf("day");
      }

      if (
        confDateForComparison &&
        !cfpClosingDate.isBefore(confDateForComparison, "day")
      ) {
        setCfpDateError(
          "La fin du CFP doit être avant le début de la conférence",
        );
        isValid = false;
      } else {
        setCfpDateError(null);
      }
    } else {
      setCfpDateError(null);
    }

    return isValid;
  };

  const buildConferenceDate = (): ConferenceDate => {
    switch (dateType) {
      case "single":
        return { type: "single", date: date!.format("YYYY-MM-DD") };
      case "range":
        return {
          type: "range",
          start: date!.format("YYYY-MM-DD"),
          end: dateEnd!.format("YYYY-MM-DD"),
        };
      case "month":
        return {
          type: "month",
          year: dateMonth!.year(),
          month: dateMonth!.month() + 1,
        };
    }
  };

  const onSave = (data: ConferenceFormData) => {
    if (!validateDates()) return;

    const parsedLocation = parseLocation(
      selectedLocation && selectedLocation.display_name === data.location
        ? selectedLocation
        : data.location,
    );

    let finalCfpStatus = data.cfpStatus;
    if (cfpClosingDate && cfpClosingDate.isValid()) {
      if (cfpClosingDate.isBefore(dayjs(), "day")) {
        finalCfpStatus = "Closed";
      } else {
        finalCfpStatus = "Open";
      }
    }

    onSubmit({
      id: crypto.randomUUID(),
      ...data,
      cfpStatus: finalCfpStatus,
      date: buildConferenceDate(),
      location: parsedLocation,
      ...(cfpClosingDate &&
        cfpClosingDate.isValid() && {
          cfpClosingDate: cfpClosingDate.format("YYYY-MM-DD"),
        }),
    });
    handleClose();
  };

  return (
    <DatePickerProvider>
      <Dialog
        onClose={handleClose}
        open={open}
        maxWidth="md"
        fullWidth
        aria-labelledby={DIALOG_TITLE_ID}
        slotProps={{ paper: { className: "dark:bg-slate-900 dark:bg-none" } }}
      >
        <DialogTitle
          id={DIALOG_TITLE_ID}
          className="pb-0! flex! justify-between! items-center!"
        >
          Nouvelle conférence
          <Controller
            name="cfpStatus"
            control={control}
            render={({ field }) => (
              <FormControl size="small" className="min-w-30">
                <Select
                  {...field}
                  style={{ fontWeight: "bold", color: appliedStatusColor }}
                  inputProps={{ "aria-label": "Statut du CFP" }}
                >
                  <MenuItem value="None">Pas de CFP</MenuItem>
                  <MenuItem value="Open">Open</MenuItem>
                  <MenuItem value="Closed">Closed</MenuItem>
                </Select>
              </FormControl>
            )}
          />
        </DialogTitle>

        <DialogContent>
          <p className="text-sm text-text-muted mb-6">Saisir une conférence</p>

          <Box
            component="form"
            id="create-conference-form"
            onSubmit={handleSubmit(onSave)}
            noValidate
            sx={{ display: "flex", flexDirection: "column", gap: 2 }}
          >
            <TextField
              {...register("title")}
              id="conference-title"
              label="Titre de la conférence"
              required
              fullWidth
              placeholder="Ex: Devoxx"
              error={!!errors.title}
              helperText={errors.title?.message}
            />

            <div className="grid grid-cols-2 gap-4">
              <Controller
                name="location"
                control={control}
                render={({ field }) => (
                  <Autocomplete
                    id="conference-location"
                    open={openLocation}
                    onOpen={() => setOpenLocation(true)}
                    onClose={() => setOpenLocation(false)}
                    getOptionLabel={(option) =>
                      typeof option === "string" ? option : option.display_name
                    }
                    isOptionEqualToValue={(option, value) =>
                      (typeof option === "string"
                        ? option
                        : option.display_name) ===
                      (typeof value === "string" ? value : value.display_name)
                    }
                    options={locationOptions}
                    loading={loadingLocation}
                    filterOptions={(x) => x}
                    freeSolo
                    value={field.value}
                    onChange={(event, newValue) => {
                      if (typeof newValue === "string") {
                        field.onChange(newValue);
                        setSelectedLocation(null);
                      } else if (newValue) {
                        field.onChange(newValue.display_name);
                        setSelectedLocation(newValue);
                      } else {
                        field.onChange("");
                        setSelectedLocation(null);
                      }
                    }}
                    inputValue={locationInputValue}
                    onInputChange={(event, newInputValue) => {
                      setLocationInputValue(newInputValue);
                      field.onChange(newInputValue);
                    }}
                    renderInput={(params) => (
                      <TextField
                        {...params}
                        label="Lieu"
                        required
                        fullWidth
                        placeholder="Ex: Paris, France"
                        error={!!errors.location}
                        helperText={errors.location?.message}
                        slotProps={{
                          ...params.slotProps,
                          input: {
                            ...params.slotProps.input,
                            endAdornment: (
                              <>
                                {loadingLocation ? (
                                  <CircularProgress color="inherit" size={20} />
                                ) : null}
                                {params.slotProps.input.endAdornment}
                              </>
                            ),
                          },
                        }}
                      />
                    )}
                  />
                )}
              />

              <FormControl fullWidth>
                <InputLabel id="create-date-type-label">
                  Type de date
                </InputLabel>
                <Select
                  value={dateType}
                  onChange={(e) => {
                    setDateType(e.target.value as ConferenceDate["type"]);
                    setDateError(null);
                  }}
                  labelId="create-date-type-label"
                  id="conference-date-type"
                  label="Type de date"
                >
                  <MenuItem value="single">Jour précis</MenuItem>
                  <MenuItem value="range">Plusieurs jours</MenuItem>
                  <MenuItem value="month">Mois uniquement</MenuItem>
                </Select>
              </FormControl>

              {dateType === "single" && (
                <DatePicker
                  label="Date"
                  value={date}
                  onChange={(val) => {
                    setDate(val);
                    setDateError(null);
                    setCfpDateError(null);
                  }}
                  views={["year", "month", "day"]}
                  format="DD/MM/YYYY"
                  slotProps={{
                    textField: {
                      fullWidth: true,
                      required: true,
                      id: "conference-date",
                      error: !!dateError,
                      helperText: dateError ?? undefined,
                    },
                  }}
                />
              )}

              {dateType === "range" && (
                <>
                  <DatePicker
                    label="Date de début"
                    value={date}
                    onChange={(val) => {
                      setDate(val);
                      setDateError(null);
                      setCfpDateError(null);
                    }}
                    views={["year", "month", "day"]}
                    format="DD/MM/YYYY"
                    slotProps={{
                      textField: {
                        fullWidth: true,
                        required: true,
                        id: "conference-date-start",
                        error: !!dateError,
                        helperText: dateError ?? undefined,
                      },
                    }}
                  />
                  <DatePicker
                    label="Date de fin"
                    value={dateEnd}
                    onChange={(val) => {
                      setDateEnd(val);
                      setDateError(null);
                    }}
                    views={["year", "month", "day"]}
                    format="DD/MM/YYYY"
                    minDate={date ?? undefined}
                    slotProps={{
                      textField: {
                        fullWidth: true,
                        required: true,
                        id: "conference-date-end",
                        error: !!dateError,
                      },
                    }}
                  />
                </>
              )}

              {dateType === "month" && (
                <DatePicker
                  label="Mois"
                  value={dateMonth}
                  onChange={(val) => {
                    setDateMonth(val);
                    setDateError(null);
                    setCfpDateError(null);
                  }}
                  views={["year", "month"]}
                  format="MMMM YYYY"
                  slotProps={{
                    textField: {
                      fullWidth: true,
                      required: true,
                      id: "conference-date-month",
                      error: !!dateError,
                      helperText: dateError ?? undefined,
                    },
                  }}
                />
              )}

              <TextField
                {...register("cfpLink")}
                id="conference-cfp-link"
                label="Lien CFP"
                placeholder="https://..."
                fullWidth
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <Library size={16} aria-hidden="true" />
                      </InputAdornment>
                    ),
                    endAdornment:
                      cfpLink && isValidUrl(cfpLink) ? (
                        <InputAdornment position="end">
                          <IconButton
                            size="small"
                            component="a"
                            href={cfpLink}
                            target="_blank"
                            rel="noopener noreferrer"
                            aria-label="Ouvrir le lien CFP"
                            className="text-primary! p-0.5!"
                          >
                            <ExternalLinkIcon size={14} aria-hidden="true" />
                          </IconButton>
                        </InputAdornment>
                      ) : null,
                  },
                }}
              />

              <DatePicker
                label="Date de fin du CFP"
                value={cfpClosingDate}
                onChange={(val) => {
                  setCfpClosingDate(val);
                  setCfpDateError(null);
                }}
                views={["year", "month", "day"]}
                format="DD/MM/YYYY"
                slotProps={{
                  textField: {
                    fullWidth: true,
                    id: "conference-cfp-closing-date",
                    error: !!cfpDateError,
                    helperText: cfpDateError ?? undefined,
                  },
                }}
              />
            </div>
          </Box>
        </DialogContent>

        <DialogActions className="px-6! pb-6! pt-5!">
          <Button variant="outlined" onClick={handleClose}>
            Annuler
          </Button>
          <div className="flex-1" />
          <Button
            variant="contained"
            type="submit"
            form="create-conference-form"
          >
            Créer la conférence
          </Button>
        </DialogActions>
      </Dialog>
    </DatePickerProvider>
  );
}
