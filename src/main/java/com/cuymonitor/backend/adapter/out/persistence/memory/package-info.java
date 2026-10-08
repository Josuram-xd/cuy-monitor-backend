/**
 * Temporary in-memory implementations of the health repositories. They live here until
 * cuy-monitor-db creates the health tables (V4); then the JPA adapters replace them. Data is lost on restart.
 */
package com.cuymonitor.backend.adapter.out.persistence.memory;
