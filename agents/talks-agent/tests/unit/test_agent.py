import pytest
from pydantic import ValidationError

from app.agent import (
    ReviewPoint,
    TalkReviewInput,
    TalkReviewOutput,
    app,
    root_agent,
)


def test_app_wraps_root_agent():
    # Le nom doit rester identique à root_agent_name dans agents-cli-manifest.yaml
    assert root_agent.name == "talks_agent"
    assert app.root_agent is root_agent


def test_agent_uses_review_schemas():
    assert root_agent.input_schema is TalkReviewInput
    assert root_agent.output_schema is TalkReviewOutput


def test_input_requires_title_and_abstract():
    data = TalkReviewInput(title="Optimiser les agents IA", abstract="Un cadre pour...")
    assert data.title == "Optimiser les agents IA"
    with pytest.raises(ValidationError):
        TalkReviewInput(title="Sans abstract")


def test_review_point_requires_three_suggestions():
    with pytest.raises(ValidationError):
        ReviewPoint(category="titre", comment="Trop long", suggestions=["a", "b"])


def test_review_point_rejects_unknown_category():
    with pytest.raises(ValidationError):
        ReviewPoint(category="style", comment="...", suggestions=["a", "b", "c"])
