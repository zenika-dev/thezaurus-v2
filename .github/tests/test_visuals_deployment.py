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
    def render(self, **overrides):
        deploy = workflow("_deploy.yml")
        step = next(s for s in deploy["jobs"]["deploy"]["steps"] if s.get("name") == "Rendre le manifeste Cloud Run")
        values = {
            "SERVICE_NAME": "thezaurus-test", "VERSION": "2026.11.19-abc1234",
            "GCP_REGION": "europe-west1", "GCP_PROJECT_ID": "test-project",
            "RUNTIME_SERVICE_ACCOUNT": "runtime@test-project.iam.gserviceaccount.com",
            "NEXTAUTH_URL": "https://thezaurus.example.test", "GOOGLE_CLIENT_ID": "test.apps.googleusercontent.com",
            "FIRESTORE_DATABASE_ID": "test", "FIRESTORE_COLLECTION_PREFIX": "test", "PROFILE_PHOTOS_BUCKET": "test-photos",
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
            rendered = Path(directory, "service.rendered.yaml").read_text(encoding="utf-8")
        self.assertNotRegex(rendered, r"\$\{")
        return yaml.load(rendered, Loader=yaml.BaseLoader)

    def test_private_sidecar_identity_probes_and_shared_secret(self):
        service = self.render()
        template = service["spec"]["template"]
        spec = template["spec"]
        containers = {c["name"]: c for c in spec["containers"]}
        self.assertEqual(set(containers), {"front", "api", "visuals-agent"})
        self.assertEqual([c["name"] for c in containers.values() if "ports" in c], ["front"])
        dependencies = json.loads(template["metadata"]["annotations"]["run.googleapis.com/container-dependencies"])
        self.assertEqual(dependencies, {"front": ["api"], "api": ["visuals-agent"]})
        self.assertEqual(spec["serviceAccountName"], "runtime@test-project.iam.gserviceaccount.com")
        envs = {name: {e["name"]: e for e in c["env"]} for name, c in containers.items()}
        self.assertEqual(envs["api"]["VISUALS_AGENT_URL"]["value"], "http://localhost:8087")
        self.assertEqual(envs["api"]["VISUALS_AGENT_TIMEOUT"]["value"], "PT120S")
        self.assertEqual(envs["api"]["VISUALS_API_KEY"]["valueFrom"], envs["visuals-agent"]["VISUALS_API_KEY"]["valueFrom"])
        self.assertEqual(envs["api"]["VISUALS_API_KEY"]["valueFrom"]["secretKeyRef"],
                         {"name": "thezaurus-visuals-api-key", "key": "1"})
        self.assertNotIn("VISUALS_API_KEY", envs["front"])
        agent = containers["visuals-agent"]
        self.assertIn("@sha256:", agent["image"])
        self.assertEqual(agent["startupProbe"]["httpGet"], {"path": "/q/health/ready", "port": "8087"})
        self.assertEqual(agent["livenessProbe"]["httpGet"]["port"], "8087")
        self.assertEqual(envs["visuals-agent"]["GOOGLE_CLOUD_PROJECT_ID"]["value"], "test-project")
        self.assertEqual(envs["visuals-agent"]["VISUALS_AI_ENABLED"]["value"], "true")
        for name in ("GOOGLE_APPLICATION_CREDENTIALS", "GOOGLE_API_KEY", "VISUALS_ADC_SOURCE"):
            self.assertNotIn(name, envs["visuals-agent"])
        self.assertNotIn("volumes", spec)

    def test_configured_model_location_timeout_and_rotation_survive_rendering(self):
        service = self.render(GEMINI_CROP_MODEL="custom-model", GOOGLE_CLOUD_LOCATION="europe-west1",
                              VISUALS_API_KEY_VERSION="7", VISUALS_AGENT_TIMEOUT="PT90S")
        containers = {c["name"]: c for c in service["spec"]["template"]["spec"]["containers"]}
        envs = {name: {e["name"]: e for e in c["env"]} for name, c in containers.items()}
        self.assertEqual(envs["api"]["VISUALS_AGENT_TIMEOUT"]["value"], "PT90S")
        self.assertEqual(envs["visuals-agent"]["GEMINI_CROP_MODEL"]["value"], "custom-model")
        self.assertEqual(envs["visuals-agent"]["GOOGLE_CLOUD_LOCATION"]["value"], "europe-west1")
        for name in ("api", "visuals-agent"):
            self.assertEqual(envs[name]["VISUALS_API_KEY"]["valueFrom"]["secretKeyRef"]["key"], "7")

    def test_api_front_release_also_versions_visuals_without_talks_deployment(self):
        release = workflow("release.yml")
        jobs = release["jobs"]
        select = next(s for s in jobs["release"]["steps"] if s.get("id") == "services")
        for app, talks, expected in [("true", "false", "api front agents"),
                                     ("true", "true", "api front agents"), ("false", "true", "agents")]:
            with self.subTest(app=app, talks=talks), tempfile.TemporaryDirectory() as directory:
                output = str(Path(directory, "output"))
                subprocess.run(["bash", "-e"], input=select["run"], text=True, check=True,
                               env={**os.environ, "API_FRONT": app, "AGENT": talks, "GITHUB_OUTPUT": output})
                self.assertEqual(Path(output).read_text().strip(), f"list={expected}")
        self.assertIn("image-visuals-agent", jobs["deploy-production"]["needs"])
        self.assertEqual(jobs["deploy-production"]["with"]["visuals_image"], "${{ needs.image-visuals-agent.outputs.image }}")
        self.assertIn("inputs.api_front", jobs["image-visuals-agent"]["if"])
        self.assertNotIn("inputs.agent", jobs["image-visuals-agent"]["if"])
        self.assertIn("inputs.agent", jobs["deploy-agent"]["if"])
        self.assertIn("inputs.api_front", workflow("ci.yml")["jobs"]["visuals-agent"]["if"])

    def test_reusable_image_contract_and_smoke_gate(self):
        build = workflow("_image-visuals-agent.yml")["jobs"]["build"]
        self.assertEqual(build["env"]["IMAGE"], "${{ vars.GCP_REGISTRY }}/visuals-agent")
        docker = next(s for s in build["steps"] if s.get("uses", "").startswith("docker/build-push-action@"))
        self.assertEqual(docker["with"]["context"], "./agents/visuals-agent")
        self.assertEqual(docker["with"]["file"], "agents/visuals-agent/src/main/docker/Dockerfile.jvm")
        self.assertIn("smoke-visuals-image.py", build["steps"][-1]["run"])
        self.assertEqual(build["steps"][-1]["env"]["IMAGE"], "${{ steps.digest.outputs.image }}")
        deploy = workflow("_deploy.yml")
        self.assertEqual(deploy["on"]["workflow_call"]["inputs"]["visuals_image"]["required"], "true")


if __name__ == "__main__":
    unittest.main()
