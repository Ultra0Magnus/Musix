```markdown
# Musix Development Patterns

> Auto-generated skill from repository analysis

## Overview
This skill provides a comprehensive guide to the development patterns, coding conventions, and workflows used in the Musix Kotlin codebase. It covers file naming, import/export styles, commit practices, database schema migrations, and testing patterns, equipping contributors to work effectively within the repository.

## Coding Conventions

- **File Naming:**  
  Use PascalCase for all file names.  
  _Example:_  
  ```
  MusixDatabase.kt
  SongRepository.kt
  ```

- **Import Style:**  
  Use relative imports within the project.  
  _Example:_  
  ```kotlin
  import com.louis.musix.data.local.MusixDatabase
  ```

- **Export Style:**  
  Use named exports for classes, functions, and objects.  
  _Example:_  
  ```kotlin
  class MusixDatabase : RoomDatabase() { ... }
  ```

- **Commit Messages:**  
  Follow [Conventional Commits](https://www.conventionalcommits.org/) with prefixes such as `fix`, `build`, and `docs`.  
  _Example:_  
  ```
  fix: resolve crash on song playback
  build: update room version to 2.5.0
  docs: add usage instructions for playlist feature
  ```

## Workflows

### Room Database Schema Migration
**Trigger:** When you need to change the Room database schema (e.g., add columns, tables) and preserve user data.  
**Command:** `/new-room-migration`

1. **Update the Room database class:**  
   Edit `app/src/main/java/com/louis/musix/data/local/MusixDatabase.kt` to increment the schema version and define the migration logic.
   ```kotlin
   val MIGRATION_1_2 = object : Migration(1, 2) {
       override fun migrate(database: SupportSQLiteDatabase) {
           database.execSQL("ALTER TABLE songs ADD COLUMN genre TEXT")
       }
   }
   ```
2. **Configure schema export:**  
   In `app/build.gradle.kts`, ensure `exportSchema` is enabled and `room.schemaLocation` is set.
   ```kotlin
   kapt {
       arguments {
           arg("room.schemaLocation", "$projectDir/schemas")
       }
   }
   ```
3. **Commit the new schema file:**  
   After building the project, commit the generated schema JSON under `app/schemas/com.louis.musix.data.local.MusixDatabase/`.
   ```
   git add app/schemas/com.louis.musix.data.local.MusixDatabase/2.json
   git commit -m "build: add Room schema for version 2"
   ```

## Testing Patterns

- **Test File Naming:**  
  Test files use the pattern `*.test.*` (e.g., `SongRepository.test.kt`).

- **Testing Framework:**  
  The specific framework is not detected, but tests are written in Kotlin and likely use a standard JVM/Kotlin testing library (e.g., JUnit).

- **Example Test File:**  
  ```kotlin
  // SongRepository.test.kt
  class SongRepositoryTest {
      @Test
      fun testGetAllSongs_returnsSongs() {
          // test logic here
      }
  }
  ```

## Commands
| Command              | Purpose                                                        |
|----------------------|----------------------------------------------------------------|
| /new-room-migration  | Start a Room database schema migration workflow                |
```
