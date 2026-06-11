---
name: verify-build-after-edit
description: >-
  After substantive code or resource edits, runs a Gradle compile to catch
  Kotlin/Java syntax and type errors, then fixes failures and re-runs until
  clean or blocked. Use when editing this Android project, when the user asks
  to verify the build, or when finishing a batch of file changes.
---

# Verify build after edits

## When to run

After you change **source** that can break compilation (Kotlin, Java, XML in `app/src`, Gradle files that affect compilation, Room schema, etc.). Skip for edits that cannot affect compile (comments-only, markdown-only) unless the user asks.

## Command (this repo)

From the **repository root** (`PeiPei`), compile the default flavored debug app module:

- **Windows (PowerShell / cmd):** `.\gradlew.bat :app:compileDevDebugKotlin`
- **Unix:** `./gradlew :app:compileDevDebugKotlin`

If that task is unavailable (Gradle sync / renamed flavors), use the closest equivalent compile task for the variant you touched, or `:app:assembleDevDebug` as a heavier fallback.

## Loop

1. Run the compile command above (full output is fine; errors are usually at the end).
2. **If it succeeds:** stop; optionally tell the user the build passed.
3. **If it fails:** read the compiler/Gradle error (file, line, message). Fix the root cause in code or config. Do not hand-wave—apply a real fix.
4. Run the **same** compile command again. Repeat until success or you hit a blocker (missing SDK, network, environment). If blocked, report the exact error and what is needed from the user.

## Scope

- Prefer fixing **your** recent edits first when the error location matches.
- Do not broaden scope (no unrelated refactors) while fixing compile errors.
