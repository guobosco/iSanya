---
name: compose-unresolved-weight
description: Fixes and prevents Jetpack Compose `Unresolved reference: weight` errors by checking scope and imports. Use when Android Compose code shows weight unresolved, unresolved reference in Modifier chains, or layout scope extension errors.
---

# Compose `weight` Error Guard

## When to apply

Apply this skill when Kotlin/Compose build output contains:
- `Unresolved reference: weight`
- `Unresolved reference` on `align`, `matchParentSize`, or similar scope extensions
- errors inside `Modifier` chains in `Row`, `Column`, or `Box`

## Quick diagnosis

1. Check if the call is `Modifier.weight(...)`.
2. Check the parent scope:
   - `weight` is valid in `Row`/`Column` content scope.
   - It is not valid in unrelated scopes.
3. Check imports in the file.

## Fix checklist

- If `Modifier.weight(...)` is used, ensure:
  - `import androidx.compose.foundation.layout.weight`
- If still failing, verify the composable is inside a `Row` or `Column` scope.
- Remove impossible calls from wrong scopes (for example, outside `Row`/`Column` content).

## Common example

```kotlin
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Example() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("Left", modifier = Modifier.weight(1f))
        Text("Right")
    }
}
```

## Prevention rule

Before finishing any Compose UI edit that touches `Modifier` chains:
1. Verify scope-bound modifier functions are used in valid parent scopes.
2. Verify required extension imports exist.
3. Re-scan the edited file for unresolved references.
