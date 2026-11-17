# Bucket privé des photos de profil de l'environnement.

resource "google_storage_bucket" "photos" {
  project = var.project_id
  name    = var.profile_photos_bucket
  # GCS renvoie la localisation en majuscules : upper() évite une fausse différence au plan.
  location = upper(var.region)

  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"

  # Staging : un destroy vide puis supprime le bucket. Prod : refusé tant qu'il contient des objets.
  force_destroy = !var.protect
}

# Indispensable : le SA runtime n'a aucun rôle Storage au niveau projet.
resource "google_storage_bucket_iam_member" "runtime_photos" {
  bucket = google_storage_bucket.photos.name
  role   = "roles/storage.objectUser"
  member = "serviceAccount:${var.runtime_service_account_email}"
}
