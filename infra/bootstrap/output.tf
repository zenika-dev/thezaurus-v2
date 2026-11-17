output "state" {
  description = "Nom du bucket qui stocke les states Terraform"
  value       = google_storage_bucket.terraform_state.name
}
