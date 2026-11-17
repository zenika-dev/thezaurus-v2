# Secrets propres à chaque environnement. Les secrets communs (Slack, Mailgun) sont dans shared.
# Terraform ne gère que le conteneur : les valeurs sont ajoutées à la main (gcloud secrets versions add).

locals {
  # Clés courtes : le nom complet est "<secret_prefix>-<clé>"
  #   prod    : thezaurus-nextauth-secret
  #   staging : thezaurus-staging-nextauth-secret
  secret_keys = [
    "google-client-secret",
    "nextauth-secret",
  ]
}

resource "google_secret_manager_secret" "app" {
  for_each  = toset(local.secret_keys)
  project   = var.project_id
  secret_id = "${var.secret_prefix}-${each.key}"

  # prevent_destroy ne peut pas dépendre d'une variable : on utilise la protection de la ressource
  # pour que la prod soit protégée et staging destructible.
  deletion_protection = var.protect

  replication {
    user_managed {
      replicas {
        location = var.data_location
      }
    }
  }
}


