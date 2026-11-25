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
