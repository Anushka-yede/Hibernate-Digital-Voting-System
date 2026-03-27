from collections import defaultdict, deque
from datetime import datetime, timezone
from typing import Dict, List

import numpy as np
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="Secure Voting AI Service", version="1.0.0")

recent_votes_by_user: Dict[int, deque] = defaultdict(lambda: deque(maxlen=20))
recent_votes_global: deque = deque(maxlen=5000)


class FraudRequest(BaseModel):
    userId: int
    electionId: int
    currentTimestamp: int


class FraudResponse(BaseModel):
    suspicious: bool
    score: float
    reason: str


class AuditRequest(BaseModel):
    records: List[dict]


@app.get("/health")
def health():
    return {"status": "ok", "service": "ai-monitor"}


@app.post("/fraud/check", response_model=FraudResponse)
def fraud_check(request: FraudRequest):
    now_ms = request.currentTimestamp
    history = recent_votes_by_user[request.userId]
    history.append(now_ms)
    recent_votes_global.append(now_ms)

    # Rule-based score: rapid retries by same user in a short window are suspicious.
    score = 0.0
    reason = "normal"

    if len(history) >= 2:
        deltas = [history[i] - history[i - 1] for i in range(1, len(history))]
        fast_attempts = sum(1 for d in deltas if d < 2500)
        score += min(0.7, fast_attempts * 0.15)

    # Add lightweight anomaly pressure score if global vote velocity spikes.
    current_minute = now_ms // 60000
    minute_counts = defaultdict(int)
    for ts in recent_votes_global:
        minute_counts[ts // 60000] += 1

    counts = np.array(list(minute_counts.values())) if minute_counts else np.array([0])
    mean = float(np.mean(counts))
    std = float(np.std(counts))
    current_count = float(minute_counts[current_minute])

    if std > 0 and current_count > mean + 2.0 * std:
        score += 0.25
        reason = "global spike anomaly"

    suspicious = score > 0.65
    if suspicious and reason == "normal":
        reason = "rapid repeated voting attempts"

    return FraudResponse(suspicious=suspicious, score=round(min(score, 0.99), 3), reason=reason)


@app.get("/anomaly/summary")
def anomaly_summary():
    minute_counts = defaultdict(int)
    for ts in recent_votes_global:
        minute_counts[ts // 60000] += 1

    if not minute_counts:
        return {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "status": "insufficient_data",
            "message": "No vote telemetry collected yet"
        }

    counts = np.array(list(minute_counts.values()), dtype=float)
    latest_minute = max(minute_counts.keys())
    latest = float(minute_counts[latest_minute])
    mean = float(np.mean(counts))
    std = float(np.std(counts))
    z_score = 0.0 if std == 0 else (latest - mean) / std

    return {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "latestVotesPerMinute": latest,
        "meanVotesPerMinute": round(mean, 2),
        "stdVotesPerMinute": round(std, 2),
        "zScore": round(z_score, 2),
        "anomaly": z_score > 2.0
    }


@app.post("/audit/verify")
def audit_verify(payload: AuditRequest):
    # This endpoint can be extended with ML-assisted consistency scoring.
    valid = [r for r in payload.records if r.get("voteHash") and r.get("blockchainTxHash")]
    return {
        "received": len(payload.records),
        "consistent": len(valid),
        "inconsistent": len(payload.records) - len(valid)
    }
