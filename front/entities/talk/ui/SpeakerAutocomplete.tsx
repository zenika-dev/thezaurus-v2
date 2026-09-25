"use client";

import {
  ContributorAutocomplete,
  type ContributorAutocompleteProps,
} from "@/entities/user";
import type { SpeakerFormData } from "../schema";

export { normalizeText, areContributorsEqual as areSpeakersEqual } from "@/entities/user";

export type SpeakerAutocompleteProps = Omit<
  ContributorAutocompleteProps,
  "value" | "onChange"
> & {
  value: SpeakerFormData[];
  onChange: (speakers: SpeakerFormData[]) => void;
  kind?: string;
};

export function SpeakerAutocomplete({
  placeholder = "Rechercher ou saisir un intervenant...",
  addAnotherPlaceholder = "Ajouter un autre intervenant...",
  externalLabel = "— Intervenant externe (texte libre)",
  kind,
  ...props
}: SpeakerAutocompleteProps) {
  return (
    <ContributorAutocomplete
      placeholder={kind ? `Rechercher ou saisir un ${kind}...` : placeholder}
      addAnotherPlaceholder={kind ? `Ajouter un autre ${kind}...` : addAnotherPlaceholder}
      externalLabel={kind === "auteur" ? "— Auteur sans email (texte libre)" : externalLabel}
      {...props}
    />
  );
}
