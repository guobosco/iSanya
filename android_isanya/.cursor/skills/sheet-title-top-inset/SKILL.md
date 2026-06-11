---
name: sheet-title-top-inset
description: >-
  Reserves 24dp from the sheet content top edge to the main title on new bottom
  pull-down sheets and similar popovers in this Compose app, using
  `DialogTitleTopPadding`. Use when adding ModalBottomSheet, bottom-anchored
  Dialog, full-screen picker Dialog, or any new dropdown-style surface that
  shows a title; read this skill before implementing the layout.
---

# Sheet title top inset (24dp)

## Requirement（原文）

距离上沿预留出 24dp 的空间。只要是新增加的下拉弹窗，都这样设置。

实现上：主标题相对**弹层内容区上沿**保留 **`DialogTitleTopPadding`（24.dp）**。

Implementation uses the theme value **`DialogTitleTopPadding`** (24.dp):

- Kotlin: `import com.example.Lulu.ui.theme.DialogTitleTopPadding`
- Source: `app/src/main/java/com/example/PeiPei/ui/theme/DialogTitleInsets.kt`

Do not duplicate raw `24.dp` for this rule; import and reuse `DialogTitleTopPadding`.

## Where to apply

Apply the **24dp top inset to the first title line** (or to the scroll/header column that contains it), measured from the **inner top edge** of the sheet surface (below rounded corners), not from random outer padding.

Typical surfaces:

- `ModalBottomSheet { ... }` — first `Column`/`Row` that contains the title
- Bottom-anchored `Dialog` + `Surface` with rounded top — title column or scroll root
- Full-screen `Dialog` headers — first title block under the dialog’s own top

If the close control sits in a `Box` overlay with the title, give the title container `.padding(top = DialogTitleTopPadding)` and align the close button with the same **top** padding (e.g. `IconButton(..., Modifier.align(Alignment.TopEnd).padding(top = DialogTitleTopPadding, end = …))`).

## Compose `padding` API (required)

`Modifier.padding` **does not** allow mixing `horizontal` with `top` / `bottom` in one call.

- **Wrong:** `padding(horizontal = 16.dp, top = DialogTitleTopPadding, bottom = 8.dp)`
- **Right:** `padding(start = 16.dp, end = 16.dp, top = DialogTitleTopPadding, bottom = 8.dp)`

Alternatively wrap horizontal + vertical pieces: `padding(horizontal = 16.dp).padding(top = DialogTitleTopPadding)` when that reads clearer.

## Checklist (new sheet)

- [ ] Title’s parent uses `top = DialogTitleTopPadding` (or equivalent first-line inset).
- [ ] Uses `DialogTitleTopPadding`, not a new literal `24.dp`.
- [ ] No invalid `padding(horizontal = …, top = …)` combination.
- [ ] Close / header actions visually aligned with the same top rule when applicable.

## Out of scope

Material3 **`AlertDialog`** title padding is controlled by the component; this skill targets **custom** sheets and dialogs. If the design must match the same inset there too, use a custom `Dialog` layout instead of bolting padding onto `AlertDialog` title slots.
