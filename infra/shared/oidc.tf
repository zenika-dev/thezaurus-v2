# Workload Identity Federation : GitHub Actions s'authentifie sur GCP via OIDC, sans clé JSON.

resource "google_iam_workload_identity_pool" "github" {
  project                   = var.project_id
  workload_identity_pool_id = var.wif_pool_id
  display_name              = "GitHub actions pool"

  lifecycle {
    prevent_destroy = true
  }
}

resource "google_iam_workload_identity_pool_provider" "github" {
  project                            = var.project_id
  workload_identity_pool_id          = google_iam_workload_identity_pool.github.workload_identity_pool_id
  workload_identity_pool_provider_id = var.wif_provider_id
  display_name                       = "GitHub OIDC"

  # Seuls les workflows de ce repo peuvent obtenir un token.
  attribute_condition = "assertion.repository=='${var.github_repository}'"

  attribute_mapping = {
    "google.subject"             = "assertion.sub"
    "attribute.ref"              = "assertion.ref"
    "attribute.repository"       = "assertion.repository"
    "attribute.repository_owner" = "assertion.repository_owner"
  }

  oidc {
    issuer_uri = "https://token.actions.githubusercontent.com"
  }

  lifecycle {
    prevent_destroy = true
  }
}


# Pool dédié au SA tf-apply (Terraform en CI). Séparé du pool de déploiement :
# les bindings IAM visent un pool entier, un provider plus permissif dans le même pool contournerait la restriction.

locals {
  # Seul le workflow infra-env.yml, sur main, peut obtenir un token de ce pool.
  tf_admin_workflow_ref = "${var.github_repository}/.github/workflows/infra-env.yml@refs/heads/main"

  # Toutes les conditions doivent être vraies, sinon l'échange de token est refusé.
  tf_admin_conditions = [
    "assertion.repository == '${var.github_repository}'",
    # IDs numériques GitHub des personnes autorisées (gh api users/<login> --jq .id)
    "assertion.actor_id in ${jsonencode(var.tf_admin_actor_ids)}",
    "assertion.job_workflow_ref == '${local.tf_admin_workflow_ref}'",
    # Un re-run garde l'actor d'origine : on n'accepte que la première tentative.
    "assertion.run_attempt == '1'",
  ]
}

resource "google_iam_workload_identity_pool" "tf_admin" {
  project                   = var.project_id
  workload_identity_pool_id = "github-action-terraform"
  display_name              = "github action terraform"

  lifecycle {
    prevent_destroy = true
  }
}

resource "google_iam_workload_identity_pool_provider" "tf_admin" {
  project                            = var.project_id
  workload_identity_pool_id          = google_iam_workload_identity_pool.tf_admin.workload_identity_pool_id
  workload_identity_pool_provider_id = "github-oidc"
  display_name                       = "GitHub OIDC"

  attribute_condition = join(" && ", local.tf_admin_conditions)

  attribute_mapping = {
    "google.subject"             = "assertion.sub"
    "attribute.ref"              = "assertion.ref"
    "attribute.repository"       = "assertion.repository"
    "attribute.repository_owner" = "assertion.repository_owner"
  }

  oidc {
    issuer_uri = "https://token.actions.githubusercontent.com"
  }

  lifecycle {
    prevent_destroy = true
  }
}
