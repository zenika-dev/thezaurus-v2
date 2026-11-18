# Job quotidien qui déclenche l'envoi des rappels de feedback via l'API de l'environnement.

# Numéro du projet, pour construire l'URL déterministe du service Cloud Run.
data "google_project" "current" {
  project_id = var.project_id
}

locals {
  # Format déterministe : https://<service>-<numéro de projet>.<région>.run.app
  # Identique à l'URL actuelle du job de prod, et prévisible pour staging.
  service_url = "https://${google_cloud_run_v2_service.app.name}-${data.google_project.current.number}.${var.region}.run.app"
}

resource "google_cloud_scheduler_job" "feedback_reminder" {
  project          = var.project_id
  region           = var.region
  name             = var.scheduler_job_name
  schedule         = "0 9 * * 1-5"
  time_zone        = "Europe/Paris"
  attempt_deadline = "300s"
  paused           = var.feedback_reminder_paused

  http_target {
    http_method = "POST"
    # Chaque env appelle son propre service.
    uri = "${local.service_url}/api/internal/feedback-reminders"

    # Token OIDC vérifié par l'API (FEEDBACK_REMINDER_AUDIENCE / FEEDBACK_REMINDER_INVOKER).
    oidc_token {
      audience              = "thezaurus-feedback-reminder"
      service_account_email = var.scheduler_service_account_email
    }
  }

  retry_config {
    retry_count          = 0
    max_retry_duration   = "0s"
    min_backoff_duration = "5s"
    max_backoff_duration = "3600s"
    max_doublings        = 5
  }
}
