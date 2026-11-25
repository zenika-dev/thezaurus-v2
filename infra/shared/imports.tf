# Imports des ressources WIF créées à la main (pool dédié au SA tf-apply).
# Une fois l'apply d'import fait, ce fichier peut être supprimé : les blocs import n'ont plus d'effet.

import {
  to = google_iam_workload_identity_pool.tf_admin
  id = "projects/${var.project_id}/locations/global/workloadIdentityPools/github-action-terraform"
}

import {
  to = google_iam_workload_identity_pool_provider.tf_admin
  id = "projects/${var.project_id}/locations/global/workloadIdentityPools/github-action-terraform/providers/github-oidc"
}
