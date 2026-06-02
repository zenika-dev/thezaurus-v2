import type { TalkData } from '../../app/types/talk';

const API_URL = 'http://localhost:8080/talks';

export function mapBackendToFrontend(t: any): TalkData {
  const speakers = t.speakers || [];
  return {
    id: t.id || '',
    title: t.title || '',
    speaker: speakers[0] || '',
    cospeaker: speakers[1] || '',
    email: '',
    agency: t.office || '',
    abstract: t.description || '',
    format: 'public',
    visibility: t.visibility === 'PUBLIC' ? 'external' : 'internal',
    language: 'francais',
    conference: t.conference?.name || '',
    date: '',
    notes: '',
    status: t.status === 'DONE' ? 'Replayed' :
      t.status === 'ACCEPTED' ? 'Accepted' :
        t.status === 'SUBMITTED' ? 'Submitted' : 'Idea',
    slides: '',
    replay: '',
  };
}

export function mapFrontendToBackend(t: TalkData): any {
  const speakers = [];
  if (t.speaker.trim()) speakers.push(t.speaker.trim());
  if (t.cospeaker.trim()) speakers.push(t.cospeaker.trim());

  let backendStatus = 'PLANNED';
  if (t.status === 'Submitted') backendStatus = 'SUBMITTED';
  else if (t.status === 'Accepted') backendStatus = 'ACCEPTED';
  else if (t.status === 'Replayed') backendStatus = 'DONE';

  return {
    id: t.id,
    title: t.title,
    description: t.abstract,
    speakers: speakers,
    office: t.agency,
    conference: t.conference ? { name: t.conference } : null,
    status: backendStatus,
    visibility: t.visibility === 'external' ? 'PUBLIC' : 'PRIVATE',
  };
}

export const talkApi = {
  getTalks: async (): Promise<TalkData[]> => {
    const response = await fetch(API_URL);
    if (!response.ok) throw new Error('Failed to fetch talks');
    const data = await response.json();
    return data.map(mapBackendToFrontend);
  },

  createTalk: async (talk: TalkData): Promise<TalkData> => {
    const response = await fetch(API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(mapFrontendToBackend(talk)),
    });
    if (!response.ok) throw new Error('Failed to create talk');
    const data = await response.json();
    return mapBackendToFrontend(data);
  },

  updateTalk: async (talk: TalkData): Promise<TalkData> => {
    const response = await fetch(`${API_URL}/${talk.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(mapFrontendToBackend(talk)),
    });
    if (!response.ok) throw new Error('Failed to update talk');
    return talk;
  },

  deleteTalk: async (id: string): Promise<void> => {
    const response = await fetch(`${API_URL}/${id}`, {
      method: 'DELETE',
    });
    if (!response.ok) throw new Error('Failed to delete talk');
  }
};
