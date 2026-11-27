locals {
  secret_keys = [
    "thezaurus-slack-bot-token",
    "thezaurus-slack-signing-secret",
    "thezaurus-mailgun-sending-key",
  ]
}


resource "google_secret_manager_secret" "shared" {
  for_each  = toset(local.secret_keys)
  project   = var.project_id
  secret_id = each.key

  replication {
    user_managed {
      replicas {
        location = var.data_location
      }
    }
  }

  lifecycle {
    prevent_destroy = true
  }
}
# Secrets propres à chaque env (valeurs différentes prod / staging). Gérés ici et non dans le module
# pour survivre à un destroy de l'env : Terraform ne gère que le conteneur, les valeurs sont ajoutées à la main.
# Nom : "<secret_prefix>-<clé>" ; _deploy.yml les lit via SECRET_PREFIX (.github/envs/<env>.env).
locals {
  # nom du secret => région de réplication
  env_secrets = {
    "thezaurus-google-client-secret"         = var.data_location # prod (europe-west9)
    "thezaurus-nextauth-secret"              = var.data_location # prod
    "thezaurus-staging-google-client-secret" = var.region        # staging (europe-west1)
    "thezaurus-staging-nextauth-secret"      = var.region        # staging
  }
}

resource "google_secret_manager_secret" "env" {
  for_each  = local.env_secrets
  project   = var.project_id
  secret_id = each.key

  replication {
    user_managed {
      replicas {
        location = each.value
      }
    }
  }

  lifecycle {
    prevent_destroy = true
  }
}


