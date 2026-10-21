"use server";

import { talkApi, TalkReviewApiError } from "./api";
import { revalidatePath } from "next/cache";
import { TALK_REVIEW_FALLBACK_ERROR, type TalkData, type TalkReviewRequest, type TalkReviewResult } from "./model";

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

export async function reviewTalkAction(payload: TalkReviewRequest): Promise<TalkReviewResult> {
  try {
    return { ok: true, data: await talkApi.reviewTalk(payload) };
  } catch (error) {
    if (error instanceof TalkReviewApiError) return { ok: false, message: error.message };
    console.error("Talk review failed", error);
    return { ok: false, message: TALK_REVIEW_FALLBACK_ERROR };
  }
}
