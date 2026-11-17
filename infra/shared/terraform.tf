terraform {

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~>8.11"
    }

  }
  required_version = ">= 1.7"
}
