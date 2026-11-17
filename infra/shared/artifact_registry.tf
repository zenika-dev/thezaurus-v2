# Dépôt Docker des images api / front, partagé par tous les environnements.

resource "google_artifact_registry_repository" "docker" {
  project       = var.project_id
  location      = var.region
  repository_id = var.artifact_registry_repository
  format        = "DOCKER"
  description   = "Cloud Run Source Deployments"

  lifecycle {
    prevent_destroy = true
  }
}
