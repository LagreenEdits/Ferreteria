package com.xorr.arquitectura;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.List;

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

        // Deudas conocidas de 4.4: se activan despues del refactor 16.4
        @ArchIgnore(reason = "Deuda 4.4: ciclo Inventario<->Compras/Ventas y common->seguridad")
        @ArchTest
        static final ArchRule sinCiclosEntreModulos = slices().matching("com.xorr.(*)..").should().beFreeOfCycles();

        @ArchIgnore(reason = "Deuda 4.4: ContextoActual importa DatosToken")
        @ArchTest
        static final ArchRule commonNoDependeDeSeguridad = noClasses().that().resideInAPackage("com.xorr.common..")
                        .should().dependOnClassesThat().resideInAPackage("com.xorr.seguridad..");
}