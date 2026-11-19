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

variable "artifact_registry_repository" {
  description = "Nom du dépôt Artifact Registry des images"
  type        = string
  default     = "cloud-run-source-deploy"
}
variable "wif_pool_id" {
  description = "ID du pool Workload Identity (segment après workloadIdentityPools/ dans WIF_PROVIDER)"
  type        = string
  default     = "github-actions-pool"
}

variable "wif_provider_id" {
  description = "ID du provider OIDC GitHub (dernier segment de WIF_PROVIDER)"
  type        = string
  default     = "github-actions-oidc"
}

variable "github_repository" {
  description = "Repo GitHub autorisé à utiliser le pool (owner/nom)"
  type        = string
  default     = "zenika-dev/thezaurus-v2"
}

variable "data_location" {
  description = "Région des données (secrets, Firestore)"
  type        = string
  default     = "europe-west9"
}

variable "tf_admin_actor_ids" {
  description = "IDs numériques GitHub autorisés à lancer infra-env.yml (gh api users/<login> --jq .id)"
  type        = list(string)
  default     = ["290412554"]
}
