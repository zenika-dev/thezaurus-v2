# Variables du module : AUCUN default, pour qu'un oubli fasse échouer le plan
# au lieu de viser silencieusement la prod.

variable "project_id" {
  description = "Identifiant du projet GCP"
  type        = string
}

variable "data_location" {
  description = "Région des données (Firestore, secrets) : europe-west9"
  type        = string
}

variable "runtime_service_account_email" {
  description = "Email du SA runtime Cloud Run (créé dans shared)"
  type        = string
}

variable "protect" {
  description = "true en prod : active les protections contre la suppression"
  type        = bool
}

variable "firestore_database_id" {
  description = "Nom de la base Firestore : thezaurus-prod, thezaurus-staging"
  type        = string
}

variable "firestore_delete_protection" {
  description = "Protection GCP contre la suppression de la base (false en prod pour garder l'état actuel)"
  type        = bool
}

variable "profile_photos_bucket" {
  description = "Bucket privé des photos de profil"
  type        = string
}
variable "region" {
  description = "Région des ressources applicatives (Cloud Run, bucket, Scheduler) : europe-west1"
  type        = string
}

variable "service_name" {
  description = "Nom du service Cloud Run : thezaurus-v2 (prod), thezaurus-staging"
  type        = string
}

variable "scheduler_job_name" {
  description = "Nom du job Cloud Scheduler des rappels : talk-feedback-reminder (prod)"
  type        = string
}

variable "scheduler_service_account_email" {
  description = "Email du SA utilisé par Cloud Scheduler pour signer le token OIDC (créé dans shared)"
  type        = string
}

variable "feedback_reminder_paused" {
  description = "true pour suspendre le job (staging : évite d'envoyer de vrais rappels Slack / email)"
  type        = bool
}

variable "collection_prefix" {
  description = "Même valeur que FIRESTORE_COLLECTION_PREFIX : prod, staging (vide = collections sans préfixe)"
  type        = string
}
