package com.cuymonitor.backend.config;

import com.cuymonitor.backend.domain.port.out.HardwareStation;
import java.util.Set;
import java.util.function.Function;
import java.util.Map;
import com.cuymonitor.backend.domain.port.in.ProcessEventUseCase;
import com.cuymonitor.backend.domain.model.settings.Settings;
import com.cuymonitor.backend.domain.model.settings.SettingKey;
import com.cuymonitor.backend.domain.model.AlertMessages;
import com.cuymonitor.backend.application.ConfigurableEventProcessor;
import com.cuymonitor.backend.application.BaselineLearner;
import com.cuymonitor.backend.application.CageWeightEvaluator;
import com.cuymonitor.backend.application.EventProcessingService;
import com.cuymonitor.backend.domain.health.chain.BehaviorThresholdHandler;
import com.cuymonitor.backend.domain.health.chain.EventHandler;
import com.cuymonitor.backend.domain.health.chain.HandlerChainBuilder;
import com.cuymonitor.backend.domain.health.chain.IdentificationHandler;
import com.cuymonitor.backend.domain.health.chain.SustainedAnomalyHandler;
import com.cuymonitor.backend.domain.health.chain.SustainedRecoveryHandler;
import com.cuymonitor.backend.domain.health.state.EscalationPolicy;
import com.cuymonitor.backend.domain.health.chain.ValidationHandler;
import com.cuymonitor.backend.domain.notification.AlertPublisher;
import com.cuymonitor.backend.domain.port.in.RecordEventUseCase;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import com.cuymonitor.backend.domain.port.out.BaselineProfileRepository;
import com.cuymonitor.backend.domain.port.out.EventRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import com.cuymonitor.backend.domain.port.out.StateTransitionRepository;
import com.cuymonitor.backend.domain.port.out.WeightReadingRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the health core. Spring only creates the objects: the chain is linked by {@link HandlerChainBuilder}
 * (setNext, by hand), never by injected lists or @Order, so the Chain of Responsibility stays visible in code.
 */
@Configuration
@EnableConfigurationProperties(HealthProperties.class)
public class DomainConfig {

    /**
     * The health engine, built from the settings and built again when an admin changes them. The chain of handlers
     * is still wired here by hand (setNext), one engine per set of rules.
     */
    @Bean
    public ConfigurableEventProcessor eventProcessor(RecordEventUseCase recorder, GuineaPigRepository guineaPigs,
                                                     BaselineProfileRepository baselines, EventRepository events,
                                                     StateTransitionRepository transitions,
                                                     WeightReadingRepository weights, AlertRepository alerts,
                                                     AlertPublisher alertPublisher, Clock clock,
                                                     Map<SettingKey, String> settingDefaults,
                                                     HardwareStation station) {
        Function<Settings, ProcessEventUseCase> engineFor = settings -> {
            HealthProperties health = HealthProperties.from(settings);
            EventHandler chain = new HandlerChainBuilder(
                    new ValidationHandler(clock, health.maxEventAge(), health.minDetectionConfidence()),
                    new IdentificationHandler(guineaPigs),
                    new BehaviorThresholdHandler(baselines, health.anomalyThresholds(), health.baselineMinSamples()),
                    new SustainedAnomalyHandler(events, health.consecutiveAnomalyWindows(),
                            health.anomalyThresholds()),
                    new SustainedRecoveryHandler(events, health.recoveryWindows(), health.anomalyThresholds())
            ).build();
            BaselineLearner learner = new BaselineLearner(baselines, health.anomalyThresholds(),
                    health.baselineWarmupSamples());
            CageWeightEvaluator weightEvaluator = new CageWeightEvaluator(weights, alerts, alertPublisher,
                    health.weightTrendPolicy(), settings.text(SettingKey.MESSAGE_WEIGHT_DROP));
            AlertMessages messages = new AlertMessages(settings.text(SettingKey.MESSAGE_ALERT),
                    settings.text(SettingKey.MESSAGE_CRITICAL), settings.text(SettingKey.MESSAGE_DISTRESS));
            return new EventProcessingService(recorder, chain, guineaPigs, transitions, alertPublisher, clock,
                    health.distressProbability(),
                    new EscalationPolicy(health.observedToAlert(), health.alertToCritical()),
                    learner, weightEvaluator, messages);
        };
        return new ConfigurableEventProcessor(engineFor, Settings.resolve(settingDefaults, Map.of()), station);
    }
}
