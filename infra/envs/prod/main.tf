# Environnement de production : appelle le module avec les noms EXISTANTS.

locals {
  # Noms réutilisés par l'appel du module et par les imports.
  runtime_service_account_email   = "thezurus-runtime-service-accou@${var.project_id}.iam.gserviceaccount.com"
  firestore_database_id           = "thezaurus-prod"
  profile_photos_bucket           = "${var.project_id}-profile-photos-prod"
  service_name                    = "thezaurus-v2"
  scheduler_job_name              = "talk-feedback-reminder"
  scheduler_service_account_email = "thezaurus-scheduler-sa@${var.project_id}.iam.gserviceaccount.com"
}

module "app" {
  source = "../../modules/environment"

  project_id                      = var.project_id
  region                          = var.region
  data_location                   = var.data_location
  secret_prefix                   = "thezaurus"
  firestore_database_id           = local.firestore_database_id
  firestore_delete_protection     = false # état actuel de la base de prod
  profile_photos_bucket           = local.profile_photos_bucket
  runtime_service_account_email   = local.runtime_service_account_email
  service_name                    = local.service_name
  scheduler_job_name              = local.scheduler_job_name
  scheduler_service_account_email = local.scheduler_service_account_email
  feedback_reminder_paused        = false
  protect                         = true
}
