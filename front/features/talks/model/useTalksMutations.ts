import { useQueryClient, useMutation } from "@tanstack/react-query";
import type { TalkData } from "@/entities/talk";
import { queryKeys } from "@/shared/api";
import {
  createTalkAction,
  updateTalkAction,
  deleteTalkAction,
} from "@/entities/talk/actions";

export function useTalksMutations() {
  const queryClient = useQueryClient();

  const createTalk = useMutation({
    mutationFn: (talk: TalkData) => createTalkAction(talk),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.talks.all });
    },
  });

  const updateTalk = useMutation({
    mutationFn: (talk: TalkData) => updateTalkAction(talk),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.talks.all });
    },
  });

  const deleteTalk = useMutation({
    mutationFn: (id: string) => deleteTalkAction(id),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.talks.all });
    },
  });

  return {
    createTalk: createTalk.mutateAsync,
    updateTalk: updateTalk.mutateAsync,
    deleteTalk: deleteTalk.mutateAsync,
  };
}
