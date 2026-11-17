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