import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import {
  ZenikaButton,
  PageHeader,
  FilterPill,
  FilterChip,
  SectionCard,
  SectionCardSkeleton,
  FormDialog,
} from "@/shared/ui";
import { visibilityTones } from "@/shared/lib/status-tones";

describe("Generic UI Components", () => {
  describe("visibilityTones", () => {
    it("is decoupled from statusTones references", () => {
      expect(visibilityTones.internal).toBeDefined();
      expect(visibilityTones.external).toBeDefined();
      expect(visibilityTones.internal.text).toBe("#9a0530");
      expect(visibilityTones.external.text).toBe("#245e12");
    });
  });

  describe("ZenikaButton", () => {
    it("renders primary button with children", () => {
      render(<ZenikaButton variant="primary">Click me</ZenikaButton>);
      const button = screen.getByRole("button", { name: "Click me" });
      expect(button).toBeInTheDocument();
      expect(button.className).toContain("app-create-action");
    });

    it("renders secondary and outlined variants", () => {
      const { rerender } = render(<ZenikaButton variant="secondary">Secondary</ZenikaButton>);
      expect(screen.getByRole("button", { name: "Secondary" })).toBeInTheDocument();

      rerender(<ZenikaButton variant="outlined">Outlined</ZenikaButton>);
      expect(screen.getByRole("button", { name: "Outlined" })).toBeInTheDocument();
    });
  });

  describe("PageHeader", () => {
    it("renders title, description, and action slot", () => {
      render(
        <PageHeader
          title="Mon titre"
          description="Description du composant"
          action={<button type="button">Action</button>}
        />
      );
      expect(screen.getByRole("heading", { level: 1, name: "Mon titre" })).toBeInTheDocument();
      expect(screen.getByText("Description du composant")).toBeInTheDocument();
      expect(screen.getByRole("button", { name: "Action" })).toBeInTheDocument();
    });
  });

  describe("FilterPill / FilterChip", () => {
    it("renders with aria-pressed and triggers onClick", () => {
      const onClick = vi.fn();
      render(<FilterPill label="Filtre actif" active={true} onClick={onClick} />);
      const button = screen.getByRole("button", { name: "Filtre actif" });
      expect(button).toHaveAttribute("aria-pressed", "true");
      fireEvent.click(button);
      expect(onClick).toHaveBeenCalledTimes(1);
    });

    it("supports FilterChip alias", () => {
      render(<FilterChip label="Chip" active={false} onClick={() => {}} />);
      const button = screen.getByRole("button", { name: "Chip" });
      expect(button).toHaveAttribute("aria-pressed", "false");
    });
  });

  describe("SectionCard and SectionCardSkeleton", () => {
    it("renders SectionCard with title and description", () => {
      render(
        <SectionCard title="Ma Section" description="Sous-titre section">
          <div>Contenu de test</div>
        </SectionCard>
      );
      expect(screen.getByRole("heading", { level: 2, name: "Ma Section" })).toBeInTheDocument();
      expect(screen.getByText("Sous-titre section")).toBeInTheDocument();
      expect(screen.getByText("Contenu de test")).toBeInTheDocument();
    });

    it("renders SectionCardSkeleton with pulse animation", () => {
      const { container } = render(<SectionCardSkeleton rows={3} />);
      expect(container.querySelector(".animate-pulse")).toBeInTheDocument();
    });
  });

  describe("FormDialog", () => {
    it("renders title, subtitle, close button and action buttons", () => {
      const onClose = vi.fn();
      const onSubmit = vi.fn();
      render(
        <FormDialog
          open={true}
          onClose={onClose}
          title="Titre de la modale"
          subtitle="Sous-titre descriptif"
          submitLabel="Valider"
          cancelLabel="Annuler"
          onSubmit={onSubmit}
        >
          <div>Champs du formulaire</div>
        </FormDialog>
      );
      expect(screen.getByText("Titre de la modale")).toBeInTheDocument();
      expect(screen.getByRole("dialog", { name: "Titre de la modale" })).toBeInTheDocument();
      expect(screen.getByText("Sous-titre descriptif")).toBeInTheDocument();
      expect(screen.getByText("Champs du formulaire")).toBeInTheDocument();
      expect(screen.getByRole("button", { name: "Valider" })).toBeInTheDocument();
      expect(screen.getByRole("button", { name: "Annuler" })).toBeInTheDocument();

      const closeButton = screen.getByRole("button", { name: "Fermer" });
      fireEvent.click(closeButton);
      expect(onClose).toHaveBeenCalled();
      fireEvent.click(screen.getByRole("button", { name: "Valider" }));
      expect(onSubmit).toHaveBeenCalledTimes(1);
    });

    it("submits its associated form and disables submission while saving", () => {
      const onSubmit = vi.fn((event: React.FormEvent) => event.preventDefault());
      const { rerender } = render(
        <FormDialog open onClose={() => {}} title="Article" formId="article-form" submitLabel="Enregistrer">
          <form id="article-form" onSubmit={onSubmit}><input name="title" /></form>
        </FormDialog>
      );
      fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));
      expect(onSubmit).toHaveBeenCalledTimes(1);
      rerender(
        <FormDialog open onClose={() => {}} title="Article" formId="article-form" submitLabel="Enregistrer" isSubmitting>
          <form id="article-form" onSubmit={onSubmit}><input name="title" /></form>
        </FormDialog>
      );
      expect(screen.getByRole("button", { name: "Enregistrer" })).toBeDisabled();
    });

    it("preserves custom actions", () => {
      const onPreview = vi.fn();
      render(
        <FormDialog open onClose={() => {}} title="Aperçu" actions={<button onClick={onPreview}>Prévisualiser</button>}>
          Contenu
        </FormDialog>
      );
      fireEvent.click(screen.getByRole("button", { name: "Prévisualiser" }));
      expect(onPreview).toHaveBeenCalledTimes(1);
    });
  });
});
