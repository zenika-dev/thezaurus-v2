# Environnement de staging : mêmes ressources que la prod, noms distincts, tout en europe-west1.
# Créé à neuf (aucun import) et destructible (protect = false).

locals {
  # SA partagés avec la prod (créés dans shared)
  runtime_service_account_email   = "thezurus-runtime-service-accou@${var.project_id}.iam.gserviceaccount.com"
  scheduler_service_account_email = "thezaurus-scheduler-sa@${var.project_id}.iam.gserviceaccount.com"

  # Noms propres à staging : aucun ne doit être identique à la prod
  firestore_database_id = "thezaurus-staging"
  profile_photos_bucket = "${var.project_id}-profile-photos-staging"
  service_name          = "thezaurus-v2-staging"
  scheduler_job_name    = "talk-feedback-reminder-staging"
}

module "app" {
  source = "../../modules/environment"

  project_id    = var.project_id
  region        = var.region
  data_location = var.region # tout dans la même région (la prod garde ses données en europe-west9)

  secret_prefix                   = "thezaurus-staging"
  firestore_database_id           = local.firestore_database_id
  firestore_delete_protection     = false
  profile_photos_bucket           = local.profile_photos_bucket
  runtime_service_account_email   = local.runtime_service_account_email
  service_name                    = local.service_name
  scheduler_job_name              = local.scheduler_job_name
  scheduler_service_account_email = local.scheduler_service_account_email

  # Bot Slack et clé Mailgun partagés avec la prod : pas de vrais rappels depuis staging
  feedback_reminder_paused = true

  # Staging doit pouvoir être détruit puis recréé
  protect = false
}
