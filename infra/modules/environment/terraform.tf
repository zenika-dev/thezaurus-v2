# Contraintes du module : pas de backend ni de provider ici, ils viennent de l'env appelant (envs/*).
# Elles empêchent d'appeler le module avec une version incompatible de Terraform ou du provider.
terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 8.11"
    }
  }

  required_version = ">= 1.7"
}
