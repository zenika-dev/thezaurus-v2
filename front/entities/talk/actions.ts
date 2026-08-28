"use server";

import { talkApi } from "./api";
import { revalidatePath } from "next/cache";
import type { TalkData, TalkReviewRequest, TalkReviewResponse } from "./model";

export async function createTalkAction(talk: TalkData): Promise<TalkData> {
  const created = await talkApi.createTalk(talk);
  revalidatePath("/talks");
  return created;
}

export async function updateTalkAction(talk: TalkData): Promise<void> {
  await talkApi.updateTalk(talk);
  revalidatePath("/talks");
}

export async function deleteTalkAction(id: string): Promise<void> {
  await talkApi.deleteTalk(id);
  revalidatePath("/talks");
}

export async function reviewTalkAction(payload: TalkReviewRequest): Promise<TalkReviewResponse> {
    return await talkApi.reviewTalk(payload);
}
