package com.xorr.arquitectura;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.xorr", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

        // Regla 4.4(1): el controlador nunca llama repositorios
        @ArchTest
        static final ArchRule controladoresNoUsanRepositorios = noClasses().that().resideInAPackage("..controller..")
                        .should().dependOnClassesThat().resideInAPackage("..repository..");

        // Regla 4.4(2): la API no expone entidades
        @ArchTest
        static final ArchRule controladoresNoUsanEntidades = noClasses().that().resideInAPackage("..controller..")
                        .should().dependOnClassesThat().resideInAPackage("..entity..");

        @ArchTest
        static final ArchRule entidadesNoDependenDeCapasSuperiores = noClasses().that().resideInAPackage("..entity..")
                        .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..");

        // Regla 4.4(3): un servicio usa solo los repositorios de su modulo
        @ArchTest
        static void serviciosSoloUsanRepositoriosDeSuModulo(JavaClasses clases) {
                List<String> modulos = List.of("inventario", "compras", "ventas", "seguridad", "negocio");
                for (String modulo : modulos) {
                        String[] ajenos = modulos.stream()
                                        .filter(m -> !m.equals(modulo))
                                        .map(m -> "com.xorr." + m + ".repository..")
                                        .toArray(String[]::new);
                        noClasses().that().resideInAPackage("com.xorr." + modulo + ".service..")
                                        .should().dependOnClassesThat().resideInAnyPackage(ajenos)
                                        .check(clases);
                }
        }

        // Antes eran deudas conocidas de 4.4; se activaron tras el refactor 16.4
        @ArchTest
        static final ArchRule sinCiclosEntreModulos = slices().matching("com.xorr.(*)..").should().beFreeOfCycles();

        @ArchTest
        static final ArchRule commonNoDependeDeSeguridad = noClasses().that().resideInAPackage("com.xorr.common..")
                        .should().dependOnClassesThat().resideInAPackage("com.xorr.seguridad..");

        // Regla nueva (Iteración 2): un servicio no depende de más de 4 colaboradores
        // en su constructor.
        // ProductoService (5) queda fuera a propósito: deuda técnica declarada, no se
        // trata hoy.
        @ArchTest
        static final ArchRule serviciosConPocasDependenciasEnElConstructor = classes()
                        .that().resideInAPackage("..service..")
                        .and().areNotInterfaces()
                        .and().doNotHaveSimpleName("ProductoService")
                        .should(new ArchCondition<JavaClass>("tener como máximo 4 parámetros en el constructor") {
                                @Override
                                public void check(JavaClass clase, ConditionEvents eventos) {
                                        for (JavaConstructor constructor : clase.getConstructors()) {
                                                int cantidad = constructor.getRawParameterTypes().size();
                                                if (cantidad > 4) {
                                                        eventos.add(SimpleConditionEvent.violated(constructor,
                                                                        clase.getSimpleName() + " tiene " + cantidad
                                                                                        + " parámetros en el constructor"));
                                                }
                                        }
                                }
                        });
}