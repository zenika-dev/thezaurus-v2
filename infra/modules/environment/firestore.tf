# Base Firestore de l'environnement et son index composite.

resource "google_firestore_database" "app" {
  project     = var.project_id
  name        = var.firestore_database_id
  location_id = var.data_location
  type        = "FIRESTORE_NATIVE"

  concurrency_mode            = "PESSIMISTIC"
  app_engine_integration_mode = "DISABLED"

  # Côté GCP : bloque toute suppression (console comprise). Activé en prod, désactivé en staging.
  delete_protection_state = var.firestore_delete_protection ? "DELETE_PROTECTION_ENABLED" : "DELETE_PROTECTION_DISABLED"
  # Côté Terraform : en prod, un destroy retire la base du state sans la supprimer.
  deletion_policy = var.protect ? "ABANDON" : "DELETE"
}

# Index requis par TalkRepository (rappels de feedback) : égalités sur feedbackReminderSent / visibility,
# IN sur status, inégalité sur date. L'ordre des champs doit rester identique, sinon l'index est recréé.
resource "google_firestore_index" "talks_feedback_reminder" {
  project  = var.project_id
  database = google_firestore_database.app.name
  # Même règle que FirestoreCollections.java : "<prefix>_talks", ou "talks" sans préfixe.
  collection  = var.collection_prefix == "" ? "talks" : "${var.collection_prefix}_talks"
  query_scope = "COLLECTION"
  api_scope   = "ANY_API"

  fields {
    field_path = "feedbackReminderSent"
    order      = "ASCENDING"
  }
  fields {
    field_path = "status"
    order      = "ASCENDING"
  }
  fields {
    field_path = "visibility"
    order      = "ASCENDING"
  }
  fields {
    field_path = "date"
    order      = "ASCENDING"
  }
  fields {
    field_path = "__name__"
    order      = "ASCENDING"
  }
}
