# Property wiki integrity (013 follow-up)

**Status:** shipped (2026-08-30). Plan retired.
**Type:** ad-hoc plan (`.planning/quick/`)
**Policy:** [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md) (`#prop:` exact key, one cache). [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) (**Wiki link**, **Property panel**).

OS-invalid sanitization rewrites only the note-target (`WikiLinkAuthoredTarget.mapNoteTarget`); the encoded `#prop:` suffix stays intact. Wiki-token uniqueness folds only the note target, so two `#prop:` keys that differ only by case both stay live when both YAML keys exist. Tests assert the **property panel** / location, not dialog absence.
