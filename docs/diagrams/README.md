# Diagrams (Task 16.1)

Drawn with Mermaid, so GitHub renders them and they change in the same PR as the code. Class names are the
real ones; `HexagonalArchitectureTest` (ArchUnit) checks the dependency arrows of the first diagram on every build.

- [1. The hexagon](#1-the-hexagon)
- [2. Factory Method](#2-factory-method) · [3. Adapter](#3-adapter) · [4. Chain of Responsibility](#4-chain-of-responsibility)
- [5. State](#5-state) · [6. Composite](#6-composite) · [7. Observer](#7-observer) · [8. Proxy](#8-proxy)
- [9. One event, end to end](#9-one-event-end-to-end)

## 1. The hexagon

```mermaid
flowchart LR
    subgraph IN["Input adapters (adapter.in)"]
        WEB["web<br/>controllers"]
        ING["ingestion<br/>Factory Method + Adapter"]
        SCH["scheduling<br/>cleanup jobs"]
    end
    subgraph CORE["Core"]
        direction TB
        PIN(["domain.port.in<br/>use cases"])
        APP["application<br/>services"]
        DOM["domain<br/>model · health (Chain, State, Composite)<br/>notification (Observer) · access (Proxy)"]
        POUT(["domain.port.out<br/>repositories · AlertObserver · TokenIssuer"])
        PIN --> APP --> DOM
        APP --> POUT
    end
    subgraph OUT["Output adapters (adapter.out)"]
        PER["persistence<br/>JPA · in memory"]
        NOT["notification<br/>database · WebSocket · log"]
        SEC["security · mail · code"]
    end
    AI(("ai-service")) --> ING
    ARD(("Arduino bridge")) --> ING
    DASH(("dashboard")) --> WEB
    WEB --> PIN
    ING --> PIN
    SCH --> PIN
    PER -. implements .-> POUT
    NOT -. implements .-> POUT
    SEC -. implements .-> POUT
    PER --> DB[("PostgreSQL<br/>cuy-monitor-db")]
    NOT --> DASH
    CFG["config<br/>wires the beans"] -.-> IN
    CFG -.-> CORE
    CFG -.-> OUT
```

Arrows only point inwards: the core never imports an adapter, Spring or JPA.

## 2. Factory Method

```mermaid
classDiagram
    class AdapterFactory {
        <<abstract>>
        +createAdapter()* EventSourceAdapter
    }
    class CameraAdapterFactory { +createAdapter() EventSourceAdapter }
    class AudioAdapterFactory { +createAdapter() EventSourceAdapter }
    class WeightAdapterFactory { +createAdapter() EventSourceAdapter }
    class AdapterFactorySelector { +createAdapterFor(EventType) EventSourceAdapter }
    AdapterFactory <|-- CameraAdapterFactory
    AdapterFactory <|-- AudioAdapterFactory
    AdapterFactory <|-- WeightAdapterFactory
    AdapterFactorySelector --> AdapterFactory : picks one by EventType
    CameraAdapterFactory ..> CameraBehaviorAdapter : creates
    AudioAdapterFactory ..> AudioClassificationAdapter : creates
    WeightAdapterFactory ..> WeightReadingAdapter : creates
```

The only `switch` on `EventType` chooses the factory; each factory creates its own product.

## 3. Adapter

```mermaid
classDiagram
    class EventSourceAdapter {
        <<interface>>
        +adapt(IngestionEvent) HealthEvent
    }
    class CameraBehaviorAdapter
    class AudioClassificationAdapter
    class WeightReadingAdapter
    class IngestionEvent { <<record, wire format>> }
    class HealthEvent { <<record, domain>> }
    EventSourceAdapter <|.. CameraBehaviorAdapter
    EventSourceAdapter <|.. AudioClassificationAdapter
    EventSourceAdapter <|.. WeightReadingAdapter
    CameraBehaviorAdapter ..> IngestionEvent : reads
    CameraBehaviorAdapter ..> HealthEvent : returns BehaviorSignal
    IngestionController --> EventSourceAdapter
    IngestionController --> ProcessEventUseCase
```

The JSON of each producer (adaptee) becomes the one `HealthEvent` the core understands (target).

## 4. Chain of Responsibility

```mermaid
classDiagram
    class EventHandler {
        <<abstract>>
        -next EventHandler
        +setNext(EventHandler) EventHandler
        +handle(EventHandlerContext) EventHandlerContext
        #handleCurrent(EventHandlerContext)*
    }
    EventHandler <|-- ValidationHandler
    EventHandler <|-- IdentificationHandler
    EventHandler <|-- BehaviorThresholdHandler
    EventHandler <|-- SustainedAnomalyHandler
    EventHandler <|-- SustainedRecoveryHandler
    EventHandler o-- EventHandler : next
    HandlerChainBuilder ..> EventHandler : links with setNext
    EventHandler ..> EventHandlerContext
```

`ValidationHandler → IdentificationHandler → BehaviorThresholdHandler → SustainedAnomalyHandler → SustainedRecoveryHandler`.
Any handler can stop the chain (stale event, unknown guinea pig) by rejecting the context.

## 5. State

```mermaid
classDiagram
    class HealthState {
        <<interface>>
        +status() HealthStatus
        +onAnomaly(Duration timeInState, EscalationPolicy) HealthState
        +onRecovery() HealthState
        +raisesAlert() boolean
    }
    HealthState <|.. NormalState
    HealthState <|.. ObservedState
    HealthState <|.. AlertState
    HealthState <|.. CriticalState
    class GuineaPigHealthContext {
        -state HealthState
        -stateSince Instant
        +anomalyConfirmed(reason, now, policy) Optional~StateTransition~
        +recovered(now) Optional~StateTransition~
    }
    GuineaPigHealthContext o-- HealthState
    GuineaPigHealthContext ..> EscalationPolicy
```

```mermaid
stateDiagram-v2
    [*] --> NORMAL
    NORMAL --> OBSERVED : anomaly in N windows
    OBSERVED --> ALERT : still anomalous after observedToAlert
    ALERT --> CRITICAL : still anomalous after alertToCritical
    OBSERVED --> NORMAL : M normal windows
    ALERT --> NORMAL : M normal windows
    CRITICAL --> NORMAL : M normal windows
```

## 6. Composite

```mermaid
classDiagram
    class HealthComponent {
        <<interface>>
        +status() HealthStatus
    }
    HealthComponent <|.. CageHealth
    HealthComponent <|.. GuineaPigHealth
    HealthComponent <|.. CageAudioHealth
    HealthComponent <|.. CageWeightHealth
    CageHealth o-- "1..*" HealthComponent : children
```

`CageHealth.status()` is the worst status of its children; the dashboard's traffic light reads only that.

## 7. Observer

```mermaid
classDiagram
    class AlertObserver {
        <<interface, domain.port.out>>
        +onAlert(Alert)
    }
    class AlertPublisher {
        -observers List~AlertObserver~
        +publish(Alert)
    }
    AlertPublisher o-- "*" AlertObserver
    AlertObserver <|.. DatabaseAlertObserver
    AlertObserver <|.. WebSocketAlertObserver
    AlertObserver <|.. LogAlertObserver
    EventProcessingService --> AlertPublisher : on ALERT / CRITICAL
```

The order (database first, so the alert has its id; then WebSocket; then the log) is written in `NotificationConfig`.

## 8. Proxy

```mermaid
classDiagram
    class CageOperations {
        <<interface, Subject>>
        +getHealth(requester, cage)
        +listGuineaPigs(requester, cage)
        +getAlertHistory(requester, filter)
        +reviewAlert(requester, alertId)
    }
    class CageAccessProxy {
        -realSubject CageOperations
        -checkOwner(requester, cage)
    }
    class CageOperationsService { <<Real subject>> }
    CageOperations <|.. CageAccessProxy
    CageOperations <|.. CageOperationsService
    CageAccessProxy --> CageOperationsService : forwards after the check
    CageController --> CageOperations
    AlertController --> CageOperations
```

Written by hand: no `@PreAuthorize`, no Spring AOP (also checked by ArchUnit).

## 9. One event, end to end

```mermaid
sequenceDiagram
    participant AI as ai-service
    participant IC as IngestionController
    participant F as AdapterFactorySelector
    participant P as EventProcessingService
    participant C as Chain
    participant S as GuineaPigHealthContext
    participant O as AlertPublisher
    AI->>IC: POST /api/v1/ingestion/events (X-API-Key)
    IC->>F: createAdapterFor(BEHAVIOR)
    F-->>IC: CameraBehaviorAdapter
    IC->>P: process(HealthEvent)
    P->>C: handle(context)
    C-->>P: anomaly confirmed
    P->>S: anomalyConfirmed(reason, now, policy)
    S-->>P: StateTransition OBSERVED → ALERT
    P->>O: publish(Alert)
    O->>O: database → WebSocket → log
    IC-->>AI: 202 ACCEPTED
```

`HealthFlowIntegrationTest` runs exactly this path against a real Postgres.
