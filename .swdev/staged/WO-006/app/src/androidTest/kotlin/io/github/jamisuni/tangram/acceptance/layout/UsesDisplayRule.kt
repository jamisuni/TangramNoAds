package io.github.jamisuni.tangram.acceptance.layout

/**
 * Marker (design WO-006 section 1, fallback rule (ii); tasks.md WO-006 header, held-out kit rules): carried by every test class that
 * applies a display through [DisplayRule] and is parameterised over [DisplaySpec] with `@Parameters(name = "{0}")`, one spec per case.
 * The fallback's sixth Gradle call runs everything else with `notAnnotation=<this FQN>`. The held-out kit uses this same FQN: it is a
 * seam, not a helper copy. Scaffolding, no requirement token.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class UsesDisplayRule
