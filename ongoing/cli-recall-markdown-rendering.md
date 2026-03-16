# CLI Recall Question Markdown Rendering

## Context

The CLI already has `renderMarkdownToTerminal()` (in `cli/src/markdown.ts`) which uses `markdansi` to render markdown to ANSI-colored terminal output. It is used for **JustReview** prompts when showing `result.details` (note content without MCQ/spelling).

## Gap

When doing recall, markdown is **not** applied to:

1. **Spelling question stem** – `showRecallPrompt` uses `status(\`Spell: ${result.stem || '...'}\`)` (raw stem)
2. **MCQ stem** – uses `status(result.stem)` (raw stem)
3. **MCQ choices** – uses `status(\`  ${i + 1}. ${result.choices[i]}\`)` (raw choices)

The backend instructs AI to use markdown for MCQ stem and choices (`AiToolFactory.java`). The spelling stem comes from `note.createMaskedDetailsForRecall().maskedDetailsAsMarkdown()` which returns markdown with cloze masks.

## Phased Plan

### Phase 1: Spelling question stem markdown (piped mode) — DONE

- Apply `renderMarkdownToTerminal` to spelling stem in `showRecallPrompt`
- Add unit test: spelling prompt with markdown stem (e.g. `**bold**`, `*italic*`) shows ANSI codes, not raw markdown

### Phase 2: MCQ stem markdown (piped mode) — DONE

- Apply `renderMarkdownToTerminal` to MCQ stem in `showRecallPrompt`
- Add unit test: MCQ prompt with markdown stem shows ANSI codes

### Phase 3: MCQ choices markdown (piped mode) — DONE

- Apply `renderMarkdownToTerminal` to each MCQ choice in `showRecallPrompt`
- Update `formatMcqChoiceLines` to render markdown (used by both piped and TTY)
- Add unit test: MCQ prompt with markdown choices shows ANSI codes

### Phase 4: Verify TTY mode — DONE

- `showRecallPrompt` and `formatMcqChoiceLines` are shared by piped and TTY. After Phases 1–3, both modes use the same rendering.
- **Manual verification**: Run `pnpm cli` in a real terminal, type `/recall`, and confirm spelling stem, MCQ stem, and MCQ choices render markdown (bold, italic, etc.) correctly in the box. Phases 1–3 implementation applies to both piped and TTY.

---

## Phase 5: HTML content support — DONE

- Added `turndown` dependency to CLI.
- Created `htmlToMarkdown()` and `looksLikeHtml()` in `cli/src/markdown.ts`. Custom Turndown rule for `<mark>` → `**text**`.
- Extended `renderMarkdownToTerminal` to auto-detect HTML and convert via `htmlToMarkdown` before rendering. No call-site changes.
- Unit tests: `<b>bold</b>`, `<mark>[..~]</mark>`, `<p>text</p>` render correctly.

### Rationale

Recall content can be **HTML** in addition to markdown:

- **Spelling stem** comes from `ClozedString.maskedDetailsAsMarkdown()` which uses `ClozedString.forMarkdownWithMarkMasks()` and outputs markdown with `<mark>...</mark>` cloze masks. Frontend converts to HTML when rendering.
- **Note content** (details) may be stored as HTML when edited via RichMarkdownEditor (Quill); the editor converts between markdown and HTML.
- **AI-generated MCQ** stem/choices are instructed to use markdown, but may occasionally output HTML, and note context fed to AI can be HTML.

### Research summary

| Approach | Pros | Cons |
|----------|------|------|
| **HTML → markdown → ANSI** (Turndown + markdansi) | Reuses markdansi; preserves bold/italic/code; Turndown ~2.7M weekly downloads, same lib frontend uses | Adds turndown dependency to CLI |
| **html-to-text** | Strips tags to plain text; 6.6M weekly downloads | Loses bold/italic styling |
| **Custom HTML→ANSI** | No new dependency | More code; must map each tag manually |

**Chosen approach**: HTML → markdown (Turndown) → ANSI (markdansi). Content-agnostic renderer: auto-detect HTML vs markdown, convert HTML to markdown when needed, then render.

### Basic HTML tags to support

| Tag | Source | Terminal mapping |
|-----|--------|------------------|
| `<b>`, `<strong>` | Common in notes | Bold ANSI |
| `<i>`, `<em>` | Common in notes | Italic ANSI |
| `<code>` | Code snippets | Monospace/inverse ANSI |
| `<mark>` | ClozedString cloze placeholders | Strip or show as bold (placeholder text like `[..~]`) |
| `<p>`, `<br>` | Paragraphs | Newlines |
| `<a href="...">` | Links | `[text](url)` → markdansi link |
| `<ul>`, `<ol>`, `<li>` | Lists | Markdown list syntax |
| `<h1>`–`<h6>` | Headings | Markdown headings |

Turndown handles most of these by default. `<mark>` may need a custom rule (e.g. pass through as `**text**` or strip tags).

### Detection heuristic

Treat as HTML when content matches tag-like pattern: `<\w[^>]*>` or contains `</`. Avoid false positives for markdown angle-bracket links `[](url)` and `<>` in code.

### Implementation tasks

1. Add `turndown` dependency to CLI (optionally `turndown-plugin-gfm` for tables/strikethrough).
2. Create `htmlToMarkdown(content: string): string` in `cli/src/markdown.ts` using TurndownService with minimal config. Add custom rule for `<mark>` if needed.
3. Update `renderMarkdownToTerminal` to accept both markdown and HTML: if content looks like HTML, run through `htmlToMarkdown` first, then render. Alternatively, expose `renderContentToTerminal(content: string)` that handles both.
4. Add unit tests: HTML input (`<b>bold</b>`, `<mark>[..~]</mark>`, `<p>text</p>`) renders as ANSI, not raw tags.
5. Ensure spelling stem, MCQ stem, MCQ choices, and JustReview details all flow through the unified renderer (no call-site changes if `renderMarkdownToTerminal` is extended).
