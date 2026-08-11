from google.adk.agents.llm_agent import Agent
from pydantic import BaseModel, Field
from typing import Literal


class TalkReviewInput(BaseModel):
    title: str = Field(description="Titre du talk proposé")
    abstract: str = Field(description="Résumé/abstract du talk")


class ReviewPoint(BaseModel):
    category: Literal["titre", "abstract", "coherence"]
    comment: str = Field(description="Retour concret et actionnable")


class TalkReviewOutput(BaseModel):
    score: int = Field(description="Note globale sur 10", ge=0, le=10)
    strengths: list[str] = Field(description="Points forts identifiés")
    improvements: list[ReviewPoint] = Field(description="Points à améliorer, par catégorie")
    suggested_title: str | None = Field(default=None, description="Proposition de titre reformulé, si pertinent")
    suggested_abstract: str | None = Field(default=None, description="Proposition d'abstract reformulé, si pertinent")


root_agent = Agent(
    model='gemini-flash-latest',
    name='talk_agent',
    description="Relit le titre et l'abstract d'un talk de conférence et propose des améliorations structurées.",
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