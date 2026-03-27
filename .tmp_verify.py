import json
from pathlib import Path
import urllib.error
import urllib.request

BASE = "http://localhost:8080"


def req(path, method="GET", data=None, headers=None):
    headers = headers or {}
    body = None
    if data is not None:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req_obj = urllib.request.Request(BASE + path, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req_obj, timeout=20) as resp:
            return resp.status, resp.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        return exc.code, exc.read().decode("utf-8")


results = {}

status, health = req("/actuator/health")
results["health"] = {"status": status, "body": health}
print("HEALTH", status, health)

headers = {}

checks = [
    "/api/candidates/search?query=&region=&electionId=",
    "/api/candidates/regions",
    "/api/elections/regions",
    "/api/elections?scope=all&page=0&size=5",
]

for path in checks:
    s, t = req(path, headers=headers)
    results[path] = {"status": s, "body": t[:500]}
    print("CALL", path, s)
    print(t[:500])

Path(".tmp_verify_result.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
