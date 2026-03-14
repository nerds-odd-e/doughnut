# CLI Recall Question Markdown Rendering

## Context

The CLI already has `renderMarkdownToTerminal()` (in `cli/src/markdown.ts`) which uses `markdansi` to render markdown to ANSI-colored terminal output. It is used for **JustReview** prompts when showing `result.details` (note content without MCQ/spelling).

## Gap

When doing recall, markdown is **not** applied to:

1. **Spelling question stem** – `showRecallPrompt` uses `status(\`Spell: ${result.stem || '...'}\`)` (raw stem)
2. **MCQ stem** – uses `status(result.stem)` (raw stem)
3. **MCQ choices** – uses `status(\`  ${i + 1}. ${result.choices[i]}\`)` (raw choices)

The backend instructs AI to use markdown for MCQ stem and choices (`AiToolFactory.java`). The spelling stem comes from `note.getClozeDescription().clozeDetails()` which can include note details in markdown.

## Phased Plan

### Phase 1: Spelling question stem markdown (piped mode)

- Apply `renderMarkdownToTerminal` to spelling stem in `showRecallPrompt`
- Add unit test: spelling prompt with markdown stem (e.g. `**bold**`, `*italic*`) shows ANSI codes, not raw markdown

### Phase 2: MCQ stem markdown (piped mode)

- Apply `renderMarkdownToTerminal` to MCQ stem in `showRecallPrompt`
- Add unit test: MCQ prompt with markdown stem shows ANSI codes

### Phase 3: MCQ choices markdown (piped mode)

- Apply `renderMarkdownToTerminal` to each MCQ choice in `showRecallPrompt`
- Update `formatMcqChoiceLines` to render markdown (used by both piped and TTY)
- Add unit test: MCQ prompt with markdown choices shows ANSI codes

### Phase 4: Verify TTY mode

- `showRecallPrompt` and `formatMcqChoiceLines` are shared by piped and TTY. After Phases 1–3, both modes use the same rendering.
- **Manual verification**: Run `pnpm cli` in a real terminal, type `/recall`, and confirm spelling stem, MCQ stem, and MCQ choices render markdown (bold, italic, etc.) correctly in the box. Phases 1–3 implementation applies to both piped and TTY.
