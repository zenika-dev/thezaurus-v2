"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import MenuItem from "@mui/material/MenuItem";
import Skeleton from "@mui/material/Skeleton";
import TextField from "@mui/material/TextField";
import { profileApi } from "@/entities/user";
import { queryKeys } from "@/shared/api";
import { SectionCard } from "@/shared/ui";

export function ContributionStatisticsSection() {
  const [selectedYear, setSelectedYear] = useState<number | null>(null);
  const { data, isPending, isError, isFetching, refetch } = useQuery({
    queryKey: queryKeys.profile.contributions(),
    queryFn: profileApi.getContributions,
    // Recheck on return to the profile, including edits made elsewhere.
    staleTime: 0,
  });
  const years = data?.years ?? [];
  const year = years.some((counts) => counts.year === selectedYear) ? selectedYear : data?.currentYear;
  const counts = years.find((counts) => counts.year === year);
  const undatedCandidatures = data?.undatedCandidatures ?? 0;
  const undatedArticles = data?.undatedPublishedArticles ?? 0;

  return (
    <SectionCard titleId="profile-contributions-title" title="Mes contributions annuelles"
      className="max-w-3xl" aria-busy={isFetching}>
      {isPending ? (
        <div role="status" aria-label="Chargement des statistiques">
          <Skeleton width={140} height={56} />
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            {[0, 1, 2].map((key) => <Skeleton key={key} variant="rounded" height={96} />)}
          </div>
        </div>
      ) : isError || !data || !counts ? (
        <Alert severity="error" action={
          <Button color="inherit" size="small" disabled={isFetching} onClick={() => void refetch()}>
            Réessayer
          </Button>
        }>
          Impossible de charger les statistiques.
        </Alert>
      ) : (
        <div className="flex flex-col gap-4">
          <TextField select label="Année" value={year} className="sm:w-40"
            onChange={(event) => setSelectedYear(Number(event.target.value))}>
            {years.map((counts) => <MenuItem key={counts.year} value={counts.year}>{counts.year}</MenuItem>)}
          </TextField>
          <dl className="grid grid-cols-1 sm:grid-cols-3 gap-4 m-0">
            {[
              ["Articles publiés", counts.publishedArticles],
              ["Talks soumis", counts.submittedCandidatures],
              ["Talks acceptés", counts.acceptedCandidatures],
            ].map(([label, value]) => (
              <div key={label} className="rounded-xl border border-primary p-4">
                <dt className="text-sm text-text-muted">{label}</dt>
                <dd className="text-3xl font-bold text-text m-0 mt-2">{value}</dd>
              </div>
            ))}
          </dl>
          {(undatedCandidatures > 0 || undatedArticles > 0) && (
            <Alert severity="warning">
              Toutes années confondues : {undatedCandidatures} Talk{undatedCandidatures > 1 ? "s" : ""}
              {" et "}{undatedArticles} article{undatedArticles > 1 ? "s publiés" : " publié"}
              {" "}sans date exploitable sont exclus des compteurs.
            </Alert>
          )}
        </div>
      )}
    </SectionCard>
  );
}
