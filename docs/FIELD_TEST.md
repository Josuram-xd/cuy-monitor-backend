# Farm test: tuning the thresholds with real data (Task 15)

The thresholds in `application.yml` (`app.health`) were chosen without real data. This is how the farm test turns
them into measured values. It needs the system mounted on the cage (`cuy-monitor-ai-service` Task 13,
`cuy-monitor-arduino` Task 7).

## 1. Before starting

- [ ] Manual RDS snapshot (`cuy-monitor-db` Task 8.3, `docs/BACKUP.md` there): the test writes real data.
- [ ] Every guinea pig of the cage registered with its mark colour; the camera sees the feeder and the waterer.
- [ ] `/ai/health` and `/api/v1/system/status` answer; the dashboard shows the cage live.
- [ ] Write down the values of `app.health` in use (they are the "before" of the table in section 4).

## 2. During the test (at least 3 days)

A person keeps an **observation log**, a sheet with one row per thing seen:

| When (date, hour) | Guinea pig | What it did | Was it really unwell? |
|---|---|---|---|
| 2026-11-12 09:40 | Canela (RED) | Still in the corner, did not eat at feeding time | Yes |
| 2026-11-12 14:10 | Chispa (ORANGE) | Slept after eating | No |

What matters are the moments a guinea pig **was** unwell (to count the alerts that were missing) and the alerts
that arrived when it **was not** (to count the false ones).

## 3. What the system said

```sql
-- every state change, per guinea pig and day
SELECT g.mark_color, t.from_status, t.to_status, t.reason, t.occurred_at
FROM state_transition t JOIN guinea_pig g ON g.id = t.guinea_pig_id
WHERE t.occurred_at BETWEEN :from AND :to
ORDER BY t.occurred_at;

-- every alert, and how long it took to review it
SELECT a.level, a.type, coalesce(g.mark_color, 'CAGE') AS who, a.message, a.created_at,
       a.reviewed_at - a.created_at AS time_to_review
FROM alert a LEFT JOIN guinea_pig g ON g.id = a.guinea_pig_id
WHERE a.created_at BETWEEN :from AND :to
ORDER BY a.created_at;

-- windows dropped by the chain because the detection was unsure (low detectionConfidence)
SELECT date_trunc('hour', occurred_at) AS hour, count(*) AS windows,
       count(*) FILTER (WHERE (payload ->> 'detectionConfidence')::numeric < 0.5) AS unsure
FROM event WHERE type = 'BEHAVIOR' AND occurred_at BETWEEN :from AND :to
GROUP BY 1 ORDER BY 1;
```

The same alerts are in the dashboard (Alertas → Historial, 30 días) and through `GET /api/v1/alerts/history`.

## 4. Record of false positives and negatives (Task 15.2)

Each alert is matched with the log, within 1 hour:

| | Unwell in the log | Not unwell in the log |
|---|---|---|
| **Alert (ALERT/CRITICAL)** | true positive | **false positive** |
| **No alert** | **false negative** | true negative |

| Day | TP | FP | FN | Time from "unwell" to the alert | Notes |
|---|---|---|---|---|---|
| | | | | | |

## 5. What to move (Task 15.1)

| What was seen | Parameter of `app.health` | Direction |
|---|---|---|
| Alerts for a guinea pig that was only sleeping | `still-factor` (1.5) | up |
| Alerts from one odd minute | `consecutive-anomaly-windows` (3) | up |
| The AI service flags everything | `classifier-threshold` (0.7) | up |
| A sick guinea pig took too long to reach ALERT | `observed-to-alert` (30m) | down |
| Recovers and falls again within minutes | `recovery-windows` (5) | up |
| Alerts for one that is just apart from the group | `group-distance-factor` (2.0) | up |
| Windows dropped for being unsure | `min-detection-confidence` (0.5) | down, only if the colours are right |
| A new guinea pig gets alerts in its first day | `baseline-min-samples` (30) | up |

Change **one parameter at a time**, write down the before and after in the table of section 4, and keep the new
values in `application.yml` with a commit per change (`chore(config): tune ... after the farm test`).

## 6. Rehearsing without the cage

`cuy-monitor-ai-service` can replay the same situations with known answers:

```bash
python -m dev.inject_test_data --api-key "$API_KEY" behavior --scenario lethargic --windows 12 --interval 5
python -m dev.inject_test_data --api-key "$API_KEY" behavior --scenario healthy --windows 30 --interval 2
```

`lethargic` must end in ALERT/CRITICAL, `healthy` must not raise a single alert, and `recovery` must go back to
NORMAL without a new one. That checks the rules; only the farm test checks the thresholds.
