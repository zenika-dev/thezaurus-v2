"use server";

import { conferenceApi } from "./api";
import { revalidatePath } from "next/cache";
import type { ConferenceData } from "./model";

export async function createConferenceAction(conference: ConferenceData): Promise<ConferenceData> {
  const created = await conferenceApi.createConference(conference);
  revalidatePath("/conferences");
  return created;
}

export async function updateConferenceAction(conference: ConferenceData): Promise<void> {
  await conferenceApi.updateConference(conference);
  revalidatePath("/conferences");
}

export async function deleteConferenceAction(id: string): Promise<void> {
  await conferenceApi.deleteConference(id);
  revalidatePath("/conferences");
}
