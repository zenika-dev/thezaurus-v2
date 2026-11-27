variable "project_id" {
  description = "Identifiant du projet GCP"
  type        = string
  default     = "thezaurus-494709"
}

variable "region" {
  description = "Région GCP par défaut"
  type        = string
  default     = "europe-west1"
}

variable "data_location" {
  description = "Région des données (Firestore, secrets)"
  type        = string
  default     = "europe-west9"
}
