/**
 * <h2>Proxy pattern (protection proxy): who may touch a cage</h2>
 *
 * <p>Every cage belongs to the account that created it. Instead of spreading "is this cage yours?" checks over
 * controllers and services, or hiding them in framework annotations, the check lives in one hand-written proxy:</p>
 *
 * <pre>
 *   Controller ──► CageOperations  (Subject: the interface the client knows)
 *                       ▲
 *          ┌────────────┴─────────────┐
 *   CageAccessProxy  ── delegates ──► CageOperationsService (RealSubject, application package)
 *   (Proxy: checks the owner,          (does the work: cages, health, guinea pigs, alerts;
 *    then forwards or refuses)          knows nothing about who is asking)
 * </pre>
 *
 * <ul>
 *   <li>{@link com.cuymonitor.backend.domain.access.CageOperations} — <b>Subject</b>. Every operation receives the
 *       id of the account that asks ({@code requesterId}).</li>
 *   <li>{@code application.CageOperationsService} — <b>RealSubject</b>. Business logic only, no access control.</li>
 *   <li>{@link com.cuymonitor.backend.domain.access.CageAccessProxy} — <b>Proxy</b>. Same interface; holds a
 *       reference to the real subject, checks that the requester owns the cage and only then forwards the call.
 *       Otherwise it throws {@link com.cuymonitor.backend.domain.exception.CageAccessDeniedException} (HTTP 403).</li>
 * </ul>
 *
 * <p>The client (REST controllers and the WebSocket subscription check) only receives the proxy, wired in
 * {@code config.CageAccessConfig}; the real subject is never a Spring bean, so nothing can skip the check.
 * Plain Java: no Spring, no security annotations.</p>
 */
package com.cuymonitor.backend.domain.access;
