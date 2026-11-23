# Droits IAM des SAs, créés à la main avant l'IaC et repris ici dans Terraform.
# Ce fichier ne crée AUCUN nouveau droit : les blocs "import" disent à Terraform
# "ces droits existent déjà dans GCP, ajoute-les au state au lieu de les créer".

# ============================================================================
# PARTIE 1 — Rôles des SAs sur le PROJET
# Source : gcloud projects get-iam-policy thezaurus-494709
# Une ligne = un SA + un rôle.
# ============================================================================

locals {
  project_roles = {
    # SA runtime : l'identité de l'application sur Cloud Run
    "runtime-aiplatform-user"        = { member = google_service_account.runtime.member, role = "roles/aiplatform.user" }
    "runtime-datastore-user"         = { member = google_service_account.runtime.member, role = "roles/datastore.user" }
    "runtime-secretmanager-accessor" = { member = google_service_account.runtime.member, role = "roles/secretmanager.secretAccessor" }

    # SA ci_deploy (thezurus-sa) : utilisé par GitHub Actions pour builder et déployer
    "ci_deploy-artifactregistry-writer" = { member = google_service_account.ci_deploy.member, role = "roles/artifactregistry.createOnPushWriter" }
    "ci_deploy-cloudscheduler-admin"    = { member = google_service_account.ci_deploy.member, role = "roles/cloudscheduler.admin" }
    "ci_deploy-serviceaccount-user"     = { member = google_service_account.ci_deploy.member, role = "roles/iam.serviceAccountUser" }
    "ci_deploy-run-developer"           = { member = google_service_account.ci_deploy.member, role = "roles/run.developer" }

    # SA ci_agent : utilisé par GitHub Actions pour déployer l'agent
    "ci_agent-aiplatform-editor" = { member = google_service_account.ci_agent.member, role = "roles/aiplatform.editor" }

    # SA scheduler : signe le token OIDC du job de rappel
    "scheduler-cloudscheduler-admin"     = { member = google_service_account.scheduler.member, role = "roles/cloudscheduler.admin" }
    "scheduler-cloudscheduler-jobrunner" = { member = google_service_account.scheduler.member, role = "roles/cloudscheduler.jobRunner" }
  }
}

# Terraform crée une ressource par ligne de la map ci-dessus.
resource "google_project_iam_member" "sa" {
  for_each = local.project_roles
  project  = var.project_id
  role     = each.value.role
  member   = each.value.member # "serviceAccount:<email du SA>"
}

# Reprise de l'existant. Format de l'id : "<projet> <rôle> <member>"
import {
  for_each = local.project_roles
  to       = google_project_iam_member.sa[each.key]
  id       = "${var.project_id} ${each.value.role} ${each.value.member}"
}

# ============================================================================
# PARTIE 2 — Lien WIF : qui a le droit de se faire passer pour un SA
# C'est le principal ajouté à la main dans la console, onglet "Principals with access" du SA.
# Ce droit est posé SUR le SA, pas sur le projet : il n'apparaît donc pas dans la commande de la partie 1.
# ============================================================================

locals {
  # Tout workflow du repo zenika-dev/thezaurus-v2, via le pool github-actions-pool
  github_repo_principal = "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.github.name}/attribute.repository/${var.github_repository}"

  # SAs que GitHub Actions utilise (vars.WIF_SERVICE_ACCOUNT et vars.WIF_AGENT_SERVICE_ACCOUNT)
  wif_sas = {
    ci_deploy = google_service_account.ci_deploy.name # name = "projects/<projet>/serviceAccounts/<email>"
    ci_agent  = google_service_account.ci_agent.name
  }
}

resource "google_service_account_iam_member" "wif_github" {
  for_each           = local.wif_sas
  service_account_id = each.value
  role               = "roles/iam.workloadIdentityUser"
  member             = local.github_repo_principal
}

# Format de l'id : "<SA> <rôle> <member>"
import {
  for_each = local.wif_sas
  to       = google_service_account_iam_member.wif_github[each.key]
  id       = "${each.value} roles/iam.workloadIdentityUser ${local.github_repo_principal}"
}

# ============================================================================
# PARTIE 3 — SA Terraform (thezaurus-terraform-sa), utilisé par infra-env.yml
# NOUVEAUX droits (pas d'import) : ils seront créés au prochain apply.
# Périmètre : ce que gèrent envs/staging et envs/prod (modules/environment).
# bootstrap et shared restent appliqués en local : ce SA n'a aucun droit IAM admin.
# ============================================================================

locals {
  terraform_sa_project_roles = {
    "run-admin"            = "roles/run.admin"            # service Cloud Run + binding allUsers invoker
    "datastore-owner"      = "roles/datastore.owner"      # base Firestore + index
    "cloudscheduler-admin" = "roles/cloudscheduler.admin" # job de rappel
    "secretmanager-admin"  = "roles/secretmanager.admin"  # conteneurs de secrets (pas les valeurs)
    "storage-admin"        = "roles/storage.admin"        # bucket photos + son IAM + bucket de state
  }

  # actAs limité aux SAs que les envs attachent à leurs ressources (pas au niveau projet)
  terraform_sa_act_as = {
    runtime   = google_service_account.runtime.name   # template du service Cloud Run
    scheduler = google_service_account.scheduler.name # token OIDC du job Scheduler
  }
}

# Ce que le SA Terraform peut faire sur le projet
resource "google_project_iam_member" "terraform_sa" {
  for_each = local.terraform_sa_project_roles
  project  = var.project_id
  role     = each.value
  member   = google_service_account.terraform_sa.member
}

# Le SA Terraform peut attacher runtime / scheduler aux ressources qu'il crée
resource "google_service_account_iam_member" "terraform_sa_act_as" {
  for_each           = local.terraform_sa_act_as
  service_account_id = each.value
  role               = "roles/iam.serviceAccountUser"
  member             = google_service_account.terraform_sa.member
}

# Lien WIF : seuls les tokens acceptés par le pool github-action-terraform
# (toi, infra-env.yml, main, 1re tentative — cf. oidc.tf) peuvent utiliser ce SA.
resource "google_service_account_iam_member" "terraform_sa_wif" {
  service_account_id = google_service_account.terraform_sa.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.tf_admin.name}/*"
}

# Valeurs à mettre dans les variables GitHub (TF_WIF_PROVIDER, TF_SERVICE_ACCOUNT)
output "tf_wif_provider" {
  value = google_iam_workload_identity_pool_provider.tf_admin.name
}

output "tf_service_account" {
  value = google_service_account.terraform_sa.email
}
