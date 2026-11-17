resource "google_storage_bucket" "terraform_state" {
  name     = "${var.project_id}-tfstate"
  location = var.region

  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"

  versioning {
    enabled = true
  }

  force_destroy = false

  lifecycle {
    prevent_destroy = true
  }

  lifecycle_rule {
    action {
      type = "Delete"
    }
    condition {
      num_newer_versions = 10
      with_state         = "ARCHIVED"
    }
  }
}

# Renommage terraform-state -> terraform_state : déplace l'adresse dans le state au lieu de détruire/recréer.
# À supprimer après le prochain apply.
moved {
  from = google_storage_bucket.terraform-state
  to   = google_storage_bucket.terraform_state
}
