"""Check the actual release wiring and render the actual deployment shell block."""

import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

import yaml


ROOT = Path(__file__).resolve().parents[2]


def workflow(name):
    return yaml.load((ROOT / ".github/workflows" / name).read_text(encoding="utf-8"), Loader=yaml.BaseLoader)


class VisualsDeploymentTest(unittest.TestCase):
    def render(self, workflow_name="_deploy.yml", **overrides):
        deploy = workflow(workflow_name)
        step_name = "Rendre le manifeste Cloud Run" if workflow_name == "_deploy.yml" else "Rendre le manifeste du service visuels"
        step = next(s for s in deploy["jobs"]["deploy"]["steps"] if s.get("name") == step_name)
        values = {
            "SERVICE_NAME": "thezaurus-test", "VERSION": "2026.11.19-abc1234",
            "GCP_REGION": "europe-west1", "GCP_PROJECT_ID": "test-project",
            "VISUALS_RUNTIME_SERVICE_ACCOUNT": "visuals@test-project.iam.gserviceaccount.com",
            "VISUALS_AGENT_URL": "https://visuals.example.run.app",
            "IMAGE": "europe-west1-docker.pkg.dev/test/images/visuals@sha256:" + "a" * 64,
            "RUNTIME_SERVICE_ACCOUNT": "runtime@test-project.iam.gserviceaccount.com",
            "NEXTAUTH_URL": "https://thezaurus.example.test", "GOOGLE_CLIENT_ID": "test.apps.googleusercontent.com",
            "FIRESTORE_DATABASE_ID": "test", "FIRESTORE_COLLECTION_PREFIX": "test", "PROFILE_PHOTOS_BUCKET": "test-photos",
            # Present in the integrated manifest on main (Cloud Scheduler/Mailgun).
            "SCHEDULER_SERVICE_ACCOUNT": "scheduler@test-project.iam.gserviceaccount.com",
            "FEEDBACK_REMINDER_AUDIENCE": "https://thezaurus.example.test/api/internal/feedback-reminders",
            "MAILGUN_DOMAIN": "mail.example.test", "SMTP_FROM": "thezaurus@example.test",
            **{f"{name}_IMAGE": f"europe-west1-docker.pkg.dev/test/images/{name.lower()}@sha256:" + "a" * 64
               for name in ("API", "FRONT", "VISUALS")},
        }
        for key, expression in step["env"].items():
            default = re.fullmatch(r"\$\{\{ vars\.\w+ \|\| '([^']+)' \}\}", expression)
            if default:
                values[key] = default[1]
            else:
                self.assertIn(key, values, f"Add a fixture for deployment variable {key}")
        values.update(overrides)
        with tempfile.TemporaryDirectory() as directory:
            # Run the production renderer, rather than a test-specific replacement.
            subprocess.run(["bash", "-e", "-o", "pipefail"], input=step["run"], text=True,
                           cwd=directory, env={**os.environ, **values}, check=True, capture_output=True)
            rendered = Path(directory, "service.rendered.yaml" if workflow_name == "_deploy.yml" else "visuals.rendered.yaml").read_text(encoding="utf-8")
        self.assertNotRegex(rendered, r"\$\{")
        return yaml.load(rendered, Loader=yaml.BaseLoader)

    def test_application_uses_remote_agent_without_a_sidecar(self):
        template = self.render()["spec"]["template"]
        spec = template["spec"]
        containers = {c["name"]: c for c in spec["containers"]}
        self.assertEqual(set(containers), {"front", "api"})
        self.assertEqual([c["name"] for c in containers.values() if "ports" in c], ["front"])
        self.assertEqual(json.loads(template["metadata"]["annotations"]["run.googleapis.com/container-dependencies"]),
                         {"front": ["api"]})
        envs = {name: {e["name"]: e for e in c["env"]} for name, c in containers.items()}
        self.assertEqual(envs["api"]["VISUALS_AGENT_URL"]["value"], "https://visuals.example.run.app")
        self.assertEqual(envs["api"]["VISUALS_API_KEY"]["valueFrom"]["secretKeyRef"],
                         {"name": "thezaurus-visuals-api-key", "key": "1"})
        self.assertNotIn("VISUALS_API_KEY", envs["front"])
        self.assertNotIn("VISUALS_API_KEYS_JSON", envs["api"])

    def test_independent_service_is_public_https_but_requires_per_client_keys(self):
        service = self.render("_deploy-visuals.yml")
        self.assertEqual(service["metadata"]["annotations"]["run.googleapis.com/ingress"], "all")
        self.assertEqual(service["metadata"]["annotations"]["run.googleapis.com/invoker-iam-disabled"], "true")
        spec = service["spec"]["template"]["spec"]
        self.assertEqual(spec["serviceAccountName"], "visuals@test-project.iam.gserviceaccount.com")
        self.assertEqual(len(spec["containers"]), 1)
        agent = spec["containers"][0]
        self.assertEqual(agent["name"], "visuals-agent")
        self.assertEqual(agent["ports"][0]["containerPort"], "8087")
        self.assertIn("@sha256:", agent["image"])
        self.assertEqual(agent["startupProbe"]["httpGet"], {"path": "/q/health/ready", "port": "8087"})
        env = {e["name"]: e for e in agent["env"]}
        self.assertEqual(env["VISUALS_REQUIRE_API_KEY"]["value"], "true")
        self.assertEqual(env["VISUALS_API_KEYS_JSON"]["valueFrom"]["secretKeyRef"],
                         {"name": "visuals-client-api-keys", "key": "1"})
        self.assertEqual(env["GOOGLE_CLOUD_PROJECT_ID"]["value"], "test-project")
        for name in ("GOOGLE_APPLICATION_CREDENTIALS", "GOOGLE_API_KEY", "VISUALS_ADC_SOURCE", "VISUALS_API_KEY"):
            self.assertNotIn(name, env)
        self.assertNotIn("volumes", spec)

    def test_each_service_has_its_own_configuration_and_key_version(self):
        service = self.render(VISUALS_API_KEY_VERSION="7", VISUALS_AGENT_TIMEOUT="PT90S")
        api = next(c for c in service["spec"]["template"]["spec"]["containers"] if c["name"] == "api")
        env = {e["name"]: e for e in api["env"]}
        self.assertEqual(env["VISUALS_AGENT_TIMEOUT"]["value"], "PT90S")
        self.assertEqual(env["VISUALS_API_KEY"]["valueFrom"]["secretKeyRef"]["key"], "7")
        service = self.render("_deploy-visuals.yml", VISUALS_API_KEYS_VERSION="8", GEMINI_CROP_MODEL="custom-model",
                              GOOGLE_CLOUD_LOCATION="europe-west1")
        env = {e["name"]: e for e in service["spec"]["template"]["spec"]["containers"][0]["env"]}
        self.assertEqual(env["VISUALS_API_KEYS_JSON"]["valueFrom"]["secretKeyRef"]["key"], "8")
        self.assertEqual(env["GEMINI_CROP_MODEL"]["value"], "custom-model")
        self.assertEqual(env["GOOGLE_CLOUD_LOCATION"]["value"], "europe-west1")

    def test_application_visuals_and_talks_can_be_released_separately(self):
        jobs = workflow("release.yml")["jobs"]
        select = next(s for s in jobs["release"]["steps"] if s.get("id") == "services")
        for app, visuals, talks, expected in [("true", "false", "false", "api front"),
                                             ("false", "true", "false", "agents"),
                                             ("false", "false", "true", "agents"),
                                             ("true", "true", "true", "api front agents")]:
            with self.subTest(app=app, visuals=visuals, talks=talks), tempfile.TemporaryDirectory() as directory:
                output = str(Path(directory, "output"))
                subprocess.run(["bash", "-e"], input=select["run"], text=True, check=True,
                               env={**os.environ, "API_FRONT": app, "VISUALS": visuals, "AGENT": talks, "GITHUB_OUTPUT": output})
                self.assertEqual(Path(output).read_text().strip(), f"list={expected}")
        self.assertIn("deploy-visuals", jobs["deploy-production"]["needs"])
        self.assertIn("needs.deploy-visuals.result == 'skipped'", jobs["deploy-production"]["if"])
        self.assertEqual(jobs["deploy-visuals"]["with"]["image"], "${{ needs.image-visuals-agent.outputs.image }}")
        self.assertIn("inputs.visuals", jobs["image-visuals-agent"]["if"])
        self.assertNotIn("inputs.api_front", jobs["image-visuals-agent"]["if"])
        self.assertIn("inputs.visuals", workflow("ci.yml")["jobs"]["visuals-agent"]["if"])

    def test_reusable_image_contract_and_smoke_gate(self):
        build = workflow("_image-visuals-agent.yml")["jobs"]["build"]
        self.assertEqual(build["env"]["IMAGE"], "${{ vars.GCP_REGISTRY }}/visuals-agent")
        docker = next(s for s in build["steps"] if s.get("uses", "").startswith("docker/build-push-action@"))
        self.assertEqual(docker["with"]["context"], "./agents/visuals-agent")
        self.assertEqual(docker["with"]["file"], "agents/visuals-agent/src/main/docker/Dockerfile.jvm")
        self.assertIn("smoke-visuals-image.py", build["steps"][-1]["run"])
        self.assertEqual(build["steps"][-1]["env"]["IMAGE"], "${{ steps.digest.outputs.image }}")
        deploy = workflow("_deploy-visuals.yml")
        self.assertEqual(deploy["on"]["workflow_call"]["inputs"]["image"]["required"], "true")

    def test_image_lookup_handles_first_publication_without_masking_access_errors(self):
        build = workflow("_image-visuals-agent.yml")["jobs"]["build"]
        step = next(s for s in build["steps"] if s.get("id") == "check")
        for mode, expected in [("present", "true"), ("absent-tag", "false"),
                               ("absent-image", "false"), ("denied", None)]:
            with self.subTest(mode=mode), tempfile.TemporaryDirectory() as directory:
                fake = Path(directory, "gcloud")
                fake.write_text("""#!/bin/bash
case "$LOOKUP_MODE" in
  present) echo '1.2.3 sha256:abc';;
  absent-tag) exit 0;;
  absent-image) echo 'ERROR: (gcloud.artifacts.docker.tags.list) NOT_FOUND: image not found' >&2; exit 1;;
  denied) echo 'ERROR: (gcloud.artifacts.docker.tags.list) PERMISSION_DENIED: access denied' >&2; exit 1;;
esac
""")
                fake.chmod(0o755)
                output = Path(directory, "output")
                result = subprocess.run(["bash", "-e", "-o", "pipefail"], input=step["run"], text=True,
                                        cwd=directory, capture_output=True,
                                        env={**os.environ, "PATH": directory + os.pathsep + os.environ["PATH"],
                                             "LOOKUP_MODE": mode, "VERSION": "1.2.3", "IMAGE": "registry/visuals-agent",
                                             "GITHUB_OUTPUT": str(output)})
                if expected is None:
                    self.assertNotEqual(result.returncode, 0)
                    self.assertIn("PERMISSION_DENIED", result.stderr)
                    self.assertFalse(output.exists())
                else:
                    self.assertEqual(result.returncode, 0, result.stderr)
                    self.assertEqual(output.read_text().strip(), f"exists={expected}")


if __name__ == "__main__":
    unittest.main()
