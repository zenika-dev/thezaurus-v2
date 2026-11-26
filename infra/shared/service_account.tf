# Comptes de service partagés par tous les environnements (décision : mêmes SA pour prod et staging).

resource "google_service_account" "runtime" {
  project      = var.project_id
  account_id   = "thezurus-runtime-service-accou"
  display_name = "thezurus runtime Service Account"

  lifecycle {
    prevent_destroy = true
  }
}

resource "google_service_account" "ci_deploy" {
  project      = var.project_id
  account_id   = "thezurus-sa"
  display_name = "thezurus Service Account"
  description  = "manages the ci/cd"

  lifecycle {
    prevent_destroy = true
  }
}

resource "google_service_account" "ci_agent" {
  project      = var.project_id
  account_id   = "thezurus-agent-service-account"
  display_name = "thezurus agent Service Account"
  description  = "thezurus agent Service Account"

  lifecycle {
    prevent_destroy = true
  }
}

resource "google_service_account" "scheduler" {
  project      = var.project_id
  account_id   = "thezaurus-scheduler-sa"
  display_name = "thezaurus-scheduler-sa"
  description  = "cloud scheduler SA"

  lifecycle {
    prevent_destroy = true
  }
}

resource "google_service_account" "terraform_sa" {
  project      = var.project_id
  account_id   = "thezaurus-terraform-sa"
  display_name = "thezaurus-terraform-sa"
  description  = "terraform"

  lifecycle {
    prevent_destroy = true
  }
}
