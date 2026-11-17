package com.campusconnect.gateway;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Arquitetura Hexagonal (Ports & Adapters) do API Gateway / BFF.
 */
@AnalyzeClasses(packages = "com.campusconnect.gateway", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule applicationNaoDependeDeControllers =
            noClasses()
                    .that()
                    .resideInAPackage("..bff.application..")
                    .should()
                    .dependOnClassesThat()
                    .haveSimpleNameEndingWith("Controller")
                    .because("application BFF nao depende de controllers");

    @ArchTest
    static final ArchRule controllersNaoDependemDeWebClientDireto =
            noClasses()
                    .that()
                    .haveSimpleNameEndingWith("Controller")
                    .should()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("org.springframework.web.reactive.function.client.WebClient")
                    .because("controllers BFF devem usar BffService para chamadas HTTP");

    @ArchTest
    static final ArchRule filtersNaoDependemDeBffApplication =
            noClasses()
                    .that()
                    .resideInAPackage("..infrastructure.adapters.in.filter..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..bff.application..")
                    .because("filters de gateway nao acoplam ao BFF");

    @ArchTest
    static final ArchRule controllersDevemTerSufixoController =
            classes()
                    .that()
                    .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .should()
                    .haveSimpleNameEndingWith("Controller");
}
