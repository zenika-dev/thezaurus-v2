"use server";

import { talkApi } from "@/shared/api";
import { revalidatePath } from "next/cache";
import type { TalkData } from "@/entities/talk";

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
