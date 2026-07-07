import { useQueryClient, useMutation } from "@tanstack/react-query";
import type { ConferenceData } from "@/entities/conference";
import { queryKeys } from "@/shared/api";
import {
  createConferenceAction,
  updateConferenceAction,
  deleteConferenceAction,
} from "@/entities/conference/actions";

export function useConferencesMutations() {
  const queryClient = useQueryClient();
  const queryKey = queryKeys.conferences.lists();

  const createConference = useMutation({
    mutationFn: (conference: ConferenceData) => createConferenceAction(conference),
    onMutate: async (newConference) => {
      await queryClient.cancelQueries({ queryKey });
      const previous = queryClient.getQueryData<ConferenceData[]>(queryKey);
      queryClient.setQueryData<ConferenceData[]>(queryKey, (old = []) => [...old, newConference]);
      return { previous };
    },
    onError: (_err, _conference, ctx) => {
      queryClient.setQueryData(queryKey, ctx?.previous);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey });
    },
  });

  const updateConference = useMutation({
    mutationFn: (conference: ConferenceData) => updateConferenceAction(conference),
    onMutate: async (updated) => {
      await queryClient.cancelQueries({ queryKey });
      const previous = queryClient.getQueryData<ConferenceData[]>(queryKey);
      queryClient.setQueryData<ConferenceData[]>(queryKey, (old = []) =>
        old.map((p) => (p.id === updated.id ? updated : p))
      );
      return { previous };
    },
    onError: (_err, _conference, ctx) => {
      queryClient.setQueryData(queryKey, ctx?.previous);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey });
    },
  });

  const deleteConference = useMutation({
    mutationFn: (id: string) => deleteConferenceAction(id),
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey });
      const previous = queryClient.getQueryData<ConferenceData[]>(queryKey);
      queryClient.setQueryData<ConferenceData[]>(queryKey, (old = []) =>
        old.filter((p) => p.id !== id)
      );
      return { previous };
    },
    onError: (_err, _id, ctx) => {
      queryClient.setQueryData(queryKey, ctx?.previous);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey });
    },
  });

  return {
    createConference: createConference.mutateAsync,
    updateConference: updateConference.mutateAsync,
    deleteConference: deleteConference.mutateAsync,
  };
}
