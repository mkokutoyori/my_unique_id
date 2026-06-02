package org.keycloak.workflow.scheduler;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;
import org.keycloak.workflow.spi.WorkflowEngineProvider;

/**
 * Periodically advances the engine to process SLAs, reminders and escalations.
 * Uses Keycloak's {@link TimerProvider} so the job is single-fire on the cluster (NFR-1).
 */
public class SlaScheduler {

    private static final Logger LOG = Logger.getLogger(SlaScheduler.class);
    private static final String TASK_NAME = "workflow-engine-sla-tick";
    private static final long INTERVAL_MS = 30_000L;

    private final KeycloakSessionFactory factory;

    public SlaScheduler(KeycloakSessionFactory factory) { this.factory = factory; }

    public void start() {
        KeycloakSession bootstrap = factory.create();
        try {
            TimerProvider timer = bootstrap.getProvider(TimerProvider.class);
            if (timer == null) {
                LOG.warn("TimerProvider unavailable; SLA scheduler disabled");
                return;
            }
            timer.schedule(new ClusterAwareScheduledTaskRunner(factory,
                    session -> {
                        WorkflowEngineProvider engine = session.getProvider(WorkflowEngineProvider.class);
                        if (engine != null) engine.tick();
                    }, INTERVAL_MS), INTERVAL_MS, TASK_NAME);
        } finally {
            bootstrap.close();
        }
    }

    public void stop() {
        KeycloakSession s = factory.create();
        try {
            TimerProvider timer = s.getProvider(TimerProvider.class);
            if (timer != null) timer.cancelTask(TASK_NAME);
        } finally { s.close(); }
    }
}
