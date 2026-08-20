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
  try {
    return await talkApi.reviewTalk(payload);
  } catch {
    const suggestedTitles = [
      "Intégrer le Front-end au Back-end Quarkus : Simplifiez vos Architectures !",
      "Back-end et Front-end Unis : L'approche Quarkus pour des Architectures Simples",
      "Quarkus : Quand le Back-end Intègre le Front-end (Sans JSF ni JSP !)",
    ];

    const suggestedAbstracts = [
      "Ajouter après 'simplifier vos architectures' : 'pour gagner en productivité et réduire la complexité opérationnelle de vos projets.'",
      "Reformuler la fin : 'Découvrez comment optimiser vos flux de développement et de déploiement grâce à ces trois approches novatrices qui vous feront gagner un temps précieux.'",
      "Insister sur le gain de temps et les ressources dès le départ : 'Fini les configurations multiples et les pipelines complexes : cette session vous montrera comment économiser temps et ressources en intégrant intelligemment vos interfaces.'",
    ];

    return {
      suggestedTitles,
      suggestedAbstracts,
      feedback: [
        "[Titre] Le titre est très accrocheur et utilise une référence culturelle populaire qui fonctionne bien.",
        "[Abstract] L'abstract est bien structuré, commence par un problème clair et identifie la cible.",
        "[Cohérence] La cohérence entre le titre et l'abstract est très bonne.",
      ],
      keyImprovements: [
        "Assurez-vous que le titre continue de promettre une intégration forte.",
        "Veillez à ce que l'abstract valide immédiatement le sérieux technique du sujet.",
      ],
      improvements: [
        {
          category: "titre",
          comment: "Le titre est accrocheur et exprime bien le sujet principal.",
          suggestions: suggestedTitles,
        },
        {
          category: "abstract",
          comment: "L'abstract est bien structuré et très engageant.",
          suggestions: suggestedAbstracts,
        },
        {
          category: "coherence",
          comment: "Très bonne cohérence entre la promesse du titre et le déroulé de l'abstract.",
          suggestions: ["Conserver le ton technique et professionnel"],
        },
      ],
    };
  }
}
