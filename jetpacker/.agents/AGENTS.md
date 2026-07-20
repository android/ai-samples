# Code Style & Formatting Constraints

## Avoid Fully Qualified Class Names (FQCN)
When writing or modifying Kotlin, Java, or Compose files:
1. **Always Use Imports**: Never write inline Fully Qualified Class Names (e.g., `com.example.jetpacker.core.flags.FeatureFlags`, `java.time.Instant`) in function bodies, parameter lists, or property definitions.
2. **Explicit File-Level Imports**: Add explicit `import` statements at the top of the file for all referenced types, classes, and utilities.
