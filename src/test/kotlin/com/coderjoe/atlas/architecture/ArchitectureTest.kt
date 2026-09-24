package com.coderjoe.atlas.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.Test

/**
 * The package rules the layout depends on, checked against the compiled classes so a stray
 * import fails the build instead of quietly bringing back a cycle.
 *
 * `craftengine/` and `util/` sit outside the layers on purpose: anything may use them, and R1
 * already limits what `craftengine/` can reach.
 */
class ArchitectureTest {
    private val classes: JavaClasses =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("com.coderjoe.atlas")

    /** R1 */
    @Test
    fun `only the craftengine package talks to CraftEngine`() {
        noClasses().that().resideOutsideOfPackage("com.coderjoe.atlas.craftengine..")
            .should().dependOnClassesThat().resideInAPackage("net.momirealms..")
            .check(classes)
    }

    /** R2 */
    @Test
    fun `block families never import each other`() {
        val families = listOf("power", "fluid", "transport")
        for (family in families) {
            val others = families.filter { it != family }.map { "com.coderjoe.atlas.block.$it.." }
            noClasses().that().resideInAPackage("com.coderjoe.atlas.block.$family..")
                .should().dependOnClassesThat().resideInAnyPackage(*others.toTypedArray())
                .check(classes)
        }
    }

    /** R3 */
    @Test
    fun `dependencies point down the layers`() {
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Plugin").definedBy("com.coderjoe.atlas")
            .layer("Edges").definedBy(
                "com.coderjoe.atlas.listener..",
                "com.coderjoe.atlas.dialog..",
                "com.coderjoe.atlas.item..",
            )
            .layer("Data").definedBy("com.coderjoe.atlas.data..")
            .layer("Blocks").definedBy("com.coderjoe.atlas.block..")
            .whereLayer("Plugin").mayNotBeAccessedByAnyLayer()
            .whereLayer("Edges").mayOnlyBeAccessedByLayers("Plugin")
            .whereLayer("Data").mayOnlyBeAccessedByLayers("Plugin", "Edges")
            .whereLayer("Blocks").mayOnlyBeAccessedByLayers("Plugin", "Edges", "Data")
            .check(classes)
    }

    /** R4 */
    @Test
    fun `no package cycles`() {
        slices().matching("com.coderjoe.atlas.(**)").should().beFreeOfCycles().check(classes)
    }
}
