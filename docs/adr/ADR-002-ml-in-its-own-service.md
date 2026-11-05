# ADR-002: Machine learning in a separate Python service

**Status:** Accepted

**Context:** Detection, tracking and audio classification need Python libraries (OpenCV, ONNX Runtime,
scikit-learn) and models of hundreds of MB. The health rules (Chain, State, Composite) are the core of the course
project and must stay easy to test in Java.

**Decision:** The models run in `cuy-monitor-ai-service` (FastAPI). It only **measures** and sends events
(`BEHAVIOR`, `AUDIO`) to the backend; it never decides the health of a guinea pig. The backend only knows events.

**Consequences:**
- The backend has no ML dependency and its tests need no model.
- The model can change (Bedrock for the demo, YOLO26n + Random Forest later) without touching the backend,
  as long as the event contract stays the same.
- `probAnomaly` is one more signal for the backend, which still compares each window with the guinea pig's own
  baseline and asks for several windows in a row before changing its state.
