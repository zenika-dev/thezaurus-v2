from typing import Literal

from google.adk.agents.llm_agent import Agent
from pydantic import BaseModel, Field


class TalkReviewInput(BaseModel):
    title: str = Field(description="Titre du talk proposé")
    abstract: str = Field(description="Résumé/abstract du talk")


class ReviewPoint(BaseModel):
    category: Literal["titre", "abstract", "coherence"]
    comment: str = Field(description="Retour concret et actionnable")
    suggestions: list[str] = Field(
         min_length=3,
        description="Au moins 3 propositions concrètes pour corriger ce point (formulations alternatives, reformulations, etc.)"
    )
   

class TalkReviewOutput(BaseModel):
    improvements: list[ReviewPoint] = Field(description="Points à améliorer, par catégorie")


root_agent = Agent(
    model='gemini-flash-latest',
    name='talk_agent',
    description="""Relit le titre et l'abstract d'un talk de conférence et propose des améliorations structurées""",
    instruction="""Tu es un relecteur expert pour une conférence technique.
Tu reçois un titre et un abstract de talk. Évalue :
- Clarté et impact du titre
- Structure, clarté et intérêt de l'abstract pour le public visé
- Cohérence titre/abstract

Réponds uniquement selon le schéma de sortie attendu, en français, avec un ton bienveillant mais exigeant.
""",
    input_schema=TalkReviewInput,
    output_schema=TalkReviewOutput,
)