import { useQueryClient, useMutation } from "@tanstack/react-query";
import type { TalkData } from "@/entities/talk";
import { queryKeys } from "@/shared/api";
import {
  createTalkAction,
  updateTalkAction,
  deleteTalkAction,
} from "@/shared/actions";

export function useTalksMutations() {
  const queryClient = useQueryClient();
  const queryKey = queryKeys.talks.lists();

  const createTalk = useMutation({
    mutationFn: (talk: TalkData) => createTalkAction(talk),
    onMutate: async (newTalk) => {
      await queryClient.cancelQueries({ queryKey });
      const previous = queryClient.getQueryData<TalkData[]>(queryKey);
      queryClient.setQueryData<TalkData[]>(queryKey, (old = []) => [...old, newTalk]);
      return { previous };
    },
    onError: (_err, _talk, ctx) => {
      queryClient.setQueryData(queryKey, ctx?.previous);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey });
    },
  });

  const updateTalk = useMutation({
    mutationFn: (talk: TalkData) => updateTalkAction(talk),
    onMutate: async (updated) => {
      await queryClient.cancelQueries({ queryKey });
      const previous = queryClient.getQueryData<TalkData[]>(queryKey);
      queryClient.setQueryData<TalkData[]>(queryKey, (old = []) =>
        old.map((t) => (t.id === updated.id ? updated : t))
      );
      return { previous };
    },
    onError: (_err, _talk, ctx) => {
      queryClient.setQueryData(queryKey, ctx?.previous);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey });
    },
  });

  const deleteTalk = useMutation({
    mutationFn: (id: string) => deleteTalkAction(id),
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey });
      const previous = queryClient.getQueryData<TalkData[]>(queryKey);
      queryClient.setQueryData<TalkData[]>(queryKey, (old = []) =>
        old.filter((t) => t.id !== id)
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
    createTalk: createTalk.mutateAsync,
    updateTalk: updateTalk.mutateAsync,
    deleteTalk: deleteTalk.mutateAsync,
  };
}
