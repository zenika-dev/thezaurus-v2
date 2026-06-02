import { useState, useEffect, useCallback } from 'react';
import type { TalkData } from '../types/talk';
import { talkApi } from '../../src/api/talks.api';

export function useTalks() {
  const [talks, setTalks] = useState<TalkData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchTalks = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await talkApi.getTalks();
      setTalks(data);
    } catch (err) {
      console.error('Error fetching talks:', err);
      setError(err instanceof Error ? err : new Error('Unknown error fetching talks'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchTalks();
  }, [fetchTalks]);

  const createTalk = async (talk: TalkData) => {
    try {
      const newTalk = await talkApi.createTalk(talk);
      setTalks((prev) => [...prev, newTalk]);
    } catch (err) {
      console.error('Error creating talk:', err);
      throw err;
    }
  };

  const updateTalk = async (updatedTalk: TalkData) => {
    try {
      await talkApi.updateTalk(updatedTalk);
      setTalks((prev) => prev.map((t) => (t.id === updatedTalk.id ? updatedTalk : t)));
    } catch (err) {
      console.error('Error updating talk:', err);
      throw err;
    }
  };

  const deleteTalk = async (id: string) => {
    try {
      await talkApi.deleteTalk(id);
      setTalks((prev) => prev.filter((t) => t.id !== id));
    } catch (err) {
      console.error('Error deleting talk:', err);
      throw err;
    }
  };

  return {
    talks,
    loading,
    error,
    createTalk,
    updateTalk,
    deleteTalk,
    refetch: fetchTalks,
  };
}
