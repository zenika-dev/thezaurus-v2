# Environnement de production : appelle le module avec les noms EXISTANTS.

locals {
  # Noms des ressources de prod existantes (déjà dans le state) : ne pas les renommer.
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
  firestore_database_id           = local.firestore_database_id
  collection_prefix               = "prod" # = FIRESTORE_COLLECTION_PREFIX
  firestore_delete_protection     = true   # bloque toute suppression côté GCP (console, gcloud, SA)
  profile_photos_bucket           = local.profile_photos_bucket
  runtime_service_account_email   = local.runtime_service_account_email
  service_name                    = local.service_name
  scheduler_job_name              = local.scheduler_job_name
  scheduler_service_account_email = local.scheduler_service_account_email
  feedback_reminder_paused        = false
  protect                         = true
}
