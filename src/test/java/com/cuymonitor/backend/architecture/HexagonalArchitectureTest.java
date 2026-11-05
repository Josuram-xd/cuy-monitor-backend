package com.cuymonitor.backend.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

// The dependency rules of docs/ARCHITECTURE.md 3.4 and the "patterns by hand" rules of 3.8, checked on the
// compiled classes on every build. A rule that breaks names the class and the dependency that broke it.
@AnalyzeClasses(packages = "com.cuymonitor.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule theDomainIsPlainJava = noClasses()
            .that().resideInAPackage("..backend.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "com.fasterxml.jackson..", "tools.jackson..",
                    "..backend.application..", "..backend.adapter..", "..backend.config..")
            .because("the domain is the center of the hexagon and knows no framework (3.4)");

    @ArchTest
    static final ArchRule theApplicationNeverReachesAnAdapter = noClasses()
            .that().resideInAPackage("..backend.application..")
            .should().dependOnClassesThat().resideInAPackage("..backend.adapter..")
            .because("use cases talk to ports, never to their implementations (3.4)");

    @ArchTest
    static final ArchRule inputAdaptersDoNotReachOutputAdapters = noClasses()
            .that().resideInAPackage("..backend.adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage("..backend.adapter.out..", "jakarta.persistence..")
            .because("a controller goes through an input port, not straight to persistence (3.4)");

    @ArchTest
    static final ArchRule controllersOnlyCallInputPorts = noClasses()
            .that().resideInAPackage("..backend.adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage("..backend.domain.port.out..", "..backend.application..")
            .because("controllers never touch repositories or services directly (3.8)");

    @ArchTest
    static final ArchRule outputAdaptersDoNotReachInputAdaptersOrUseCases = noClasses()
            .that().resideInAPackage("..backend.adapter.out..")
            .should().dependOnClassesThat().resideInAnyPackage("..backend.adapter.in..", "..backend.application..")
            .because("an output adapter only implements an output port (3.4)");

    @ArchTest
    static final ArchRule observerIsWrittenByHand = noClasses()
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "org.springframework.context.ApplicationEventPublisher")
            .orShould().dependOnClassesThat().haveFullyQualifiedName(
                    "org.springframework.context.event.EventListener")
            .because("Observer is AlertPublisher with its own AlertObserver list, not Spring events (3.8)");

    @ArchTest
    static final ArchRule proxyIsWrittenByHand = noMethods()
            .should().beAnnotatedWith("org.springframework.security.access.prepost.PreAuthorize")
            .orShould().beAnnotatedWith("org.springframework.security.access.annotation.Secured")
            .because("access to a cage is checked by CageAccessProxy, not by security annotations (3.8)");

    @ArchTest
    static final ArchRule theChainIsLinkedByHand = noClasses()
            .should().beAnnotatedWith("org.springframework.core.annotation.Order")
            .because("HandlerChainBuilder links the handlers with setNext; @Order would hide the order (3.8)");

    // the domain is one layer (its model throws its own exceptions); between layers there is no way back
    @ArchTest
    static final ArchRule theLayersHaveNoCycles = slices()
            .matching("com.cuymonitor.backend.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule theAdaptersHaveNoCycles = slices()
            .matching("com.cuymonitor.backend.adapter.(*).(*)..")
            .should().beFreeOfCycles();
}
