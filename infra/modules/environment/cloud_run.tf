# Coquille du service : Terraform gère le nom, la région, le SA et la protection.
# Les révisions (images, env, probes, conteneurs) sont gérées par la CI (_deploy.yml).

resource "google_cloud_run_v2_service" "app" {
  project             = var.project_id
  name                = var.service_name
  location            = var.region
  ingress             = "INGRESS_TRAFFIC_ALL"
  deletion_protection = var.protect

  template {
    service_account = var.runtime_service_account_email
    containers {
      # Placeholder pour la création de staging, remplacé au premier déploiement CI.
      image = "us-docker.pkg.dev/cloudrun/container/hello"
    }
  }

  lifecycle {
    # La CI (gcloud run services replace) est propriétaire des révisions.
    ignore_changes = [template, client, client_version, labels, annotations]
  }
}

# Service public (remplace --allow-unauthenticated).
resource "google_cloud_run_v2_service_iam_member" "public_invoker" {
  project  = var.project_id
  location = var.region
  name     = google_cloud_run_v2_service.app.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
