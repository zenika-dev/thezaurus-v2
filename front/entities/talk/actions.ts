"use server";

import { talkApi } from "./api";
import { revalidatePath } from "next/cache";
import type { BackendTalkReviewResponse } from "@/shared/api";
import type { TalkData, TalkReviewRequest } from "./model";

export async function createTalkAction(talk: TalkData): Promise<TalkData> {
  const created = await talkApi.createTalk(talk);
  revalidatePath("/talks");
  return created;
}

export async function updateTalkAction(talk: TalkData): Promise<TalkData> {
  const updated = await talkApi.updateTalk(talk);
  revalidatePath("/talks");
  return updated;
}

export async function deleteTalkAction(id: string): Promise<void> {
  await talkApi.deleteTalk(id);
  revalidatePath("/talks");
}

export async function reviewTalkAction(payload: TalkReviewRequest): Promise<BackendTalkReviewResponse> {
    return await talkApi.reviewTalk(payload);
}
