# ADR-005: AI in the cloud first

**Status:** Accepted

**Context:** The farm laptop is an old Celeron with an unstable connection. Running detection there was not proven,
and the bandwidth needed to send frames was not measured.

**Decision:** The AI service runs in the cloud, next to the backend; the laptop only sends frames and audio clips
(`edge_agent`). For the demo, Amazon Bedrock analyses each 60 s window; the trained models (YOLO26n, Random Forest)
replace it later with the same event contract.

**Consequences:**
- The laptop code stays simple: capture, send, retry with backoff.
- Costs per analysed window (Bedrock) and upload bandwidth are measured during the farm test (Task 15).
- If bandwidth is the bottleneck, the detector can move to the laptop: it is ONNX and already decoupled from
  the API.
