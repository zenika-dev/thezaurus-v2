export type TalkStatus = 'Draft' | 'Idea' | 'Submitted' | 'Accepted' | 'Replayed';

export interface TalkData {
  id: string;
  title: string;
  speaker: string;
  cospeaker: string;
  email: string;
  agency: string;
  abstract: string;
  format: string;
  visibility: string;
  language: string;
  conference: string;
  date?: string;
  notes: string;
  status: TalkStatus;
  slides?: string;
  replay?: string;
}

export const agencyLabels: Record<string, string> = {
  paris: 'Paris',
  nantes: 'Nantes',
  rennes: 'Rennes',
  bordeaux: 'Bordeaux',
  lyon: 'Lyon',
  lille: 'Lille',
  grenoble: 'Grenoble',
  singapour: 'Singapour',
  montreal: 'Montréal',
};

export const visibilityLabels: Record<string, string> = {
  internal: 'Interne',
  external: 'Externe',
};

export const formatLabels: Record<string, string> = {
  video: 'Vidéo',
  training: 'Formation',
  public: 'Public',
  other: 'Autre',
};

export const languageLabels: Record<string, string> = {
  francais: 'Français',
  english: 'English',
};
