terraform {
  backend "gcs" {
    bucket = "thezaurus-494709-tfstate"
    prefix = "bootstrap"
  }

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~>8.11"
    }

  }
  required_version = ">= 1.7"
}
