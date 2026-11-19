"""Exercise the production image locally, without cloud credentials or AI calls."""

import json
import struct
import subprocess
import sys
import time
import urllib.error
import urllib.request


def smoke(image):
    key = "visuals-image-smoke-key"
    container = subprocess.check_output(
        ["docker", "run", "--detach", "--rm", "--publish", "127.0.0.1::8087",
         "--env", "VISUALS_AI_ENABLED=false", "--env", f"VISUALS_API_KEY={key}", image],
        text=True,
    ).strip()
    try:
        uid = subprocess.check_output(["docker", "exec", container, "id", "-u"], text=True).strip()
        assert uid == "10001", f"Unexpected runtime UID: {uid}"
        address = subprocess.check_output(["docker", "port", container, "8087/tcp"], text=True).strip()
        base = f"http://{address}"
        for attempt in range(60):
            try:
                with urllib.request.urlopen(base + "/q/health/ready", timeout=2):
                    break
            except (OSError, urllib.error.URLError):
                if attempt == 59:
                    raise
                time.sleep(1)

        def post(category, body, expected, authenticated=True):
            headers = {"Content-Type": "application/json"}
            if authenticated:
                headers["X-API-Key"] = key
            request = urllib.request.Request(
                base + f"/visuals/{category}/image", data=json.dumps(body).encode(), headers=headers,
            )
            try:
                response = urllib.request.urlopen(request, timeout=20)
            except urllib.error.HTTPError as error:
                response = error
            with response:
                assert response.status == expected, (category, response.status, expected)
                data = response.read()
                if expected == 200:
                    assert response.headers.get_content_type() == "image/png"
                    assert data[:8] == b"\x89PNG\r\n\x1a\n"
                    assert struct.unpack(">II", data[16:24]) == (1080, 1350 if category == "blogs" else 1080)

        talk = {"title": "Smoke test", "conference": "Zenika", "speakers": ["Alice"]}
        post("talks", talk, 401, authenticated=False)
        post("talks", talk, 200)
        post("talks", {**talk, "speakers": ["Alice", "Bob", "Charlie"]}, 200)
        post("blogs", {"title": "Smoke test", "writers": ["Alice", "Bob"]}, 200)
        post("talks", {**talk, "speakers": ["A", "B", "C", "D"]}, 400)
        post("blogs", {"title": "Smoke test", "writers": []}, 400)
        print("Production image: non-root, health, API key, talk/blog PNGs and participant limits OK")
    finally:
        subprocess.run(["docker", "rm", "--force", container], check=True, stdout=subprocess.DEVNULL)


if __name__ == "__main__":
    smoke(sys.argv[1])
