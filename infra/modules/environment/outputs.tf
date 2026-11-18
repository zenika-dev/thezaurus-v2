output "secret_names" {
  description = "Noms des secrets propres à l'env (pour _deploy.yml)"
  value       = { for k, s in google_secret_manager_secret.app : k => s.secret_id }
}

output "firestore_database_id" {
  value = google_firestore_database.app.name
}

output "profile_photos_bucket" {
  value = google_storage_bucket.photos.name
}

output "service_url" {
  description = "URL déterministe du service (NEXTAUTH_URL, THEZAURUS_PUBLIC_URL, OAuth)"
  value       = local.service_url
}
