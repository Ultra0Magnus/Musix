---
name: room-database-schema-migration
description: Workflow command scaffold for room-database-schema-migration in Musix.
allowed_tools: ["Bash", "Read", "Write", "Grep", "Glob"]
---

# /room-database-schema-migration

Use this workflow when working on **room-database-schema-migration** in `Musix`.

## Goal

Adds or updates a Room database schema migration, ensuring data is preserved across schema changes and that the migration is testable.

## Common Files

- `app/src/main/java/com/louis/musix/data/local/MusixDatabase.kt`
- `app/build.gradle.kts`
- `app/schemas/com.louis.musix.data.local.MusixDatabase/*.json`

## Suggested Sequence

1. Understand the current state and failure mode before editing.
2. Make the smallest coherent change that satisfies the workflow goal.
3. Run the most relevant verification for touched files.
4. Summarize what changed and what still needs review.

## Typical Commit Signals

- Update the Room database class (e.g., MusixDatabase.kt) to define new schema version and migration logic.
- Update build.gradle.kts to enable exportSchema and configure room.schemaLocation.
- Commit the generated schema file under app/schemas/ for the new version.

## Notes

- Treat this as a scaffold, not a hard-coded script.
- Update the command if the workflow evolves materially.