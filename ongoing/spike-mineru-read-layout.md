# Spike: `/read <book path>` — MinerU PDF layout (top three layers)

Informal spike plan. **Do not treat as shipped product work** until outcomes are reviewed. Aligns with `ongoing/book-reading-research-report.md` (PDF structure is unreliable; model/layout pipelines are on the spectrum) and `.cursor/rules/planning.mdc` (small user-visible slices, spike = learn fast, then keep or discard).

## Spike goal

Answer: **Can we use MinerU (Python, `pip install mineru`) to parse a local PDF and surface a useful “top three layers” of book layout in the Doughnut CLI via `/read <book path>`?**

**Success for the spike** (pick any that apply after trying real PDFs):

- MinerU runs locally with documented prerequisites and produces JSON (or equivalent) without blocking the whole CLI on permanent model downloads in CI (acceptable to require first-time developer setup).
- We can **define “three layers” operationally** (see below) and print a **human-readable outline** in the interactive assistant scrollback.
- We know **failure modes**: scanned PDF, wrong language, missing headings, huge books (time/memory), GPU vs CPU.

**Non-goals for this spike**

- Persisting books in Doughnut, notebooks, or the backend.
- Perfect TOC fidelity or EPUB support.
- Bundling Python/MinerU inside the Node CLI bundle (expect **subprocess** to the user’s Python).

## Research notes (Context7: MinerU)

Source: Context7 library `/opendatalab/mineru` (OpenDataLab), plus project output reference docs indexed there.

- **Role:** High-quality PDF → Markdown + structured JSON (“一站式” extraction), with layout, tables, formulas, OCR paths depending on backend.
- **Python API pattern:** `do_parse(...)` with `output_dir`, `pdf_file_names`, `pdf_bytes_list`, `p_lang_list`, `backend`, `parse_method`, flags such as `f_dump_md`, `f_dump_middle_json`, optional `start_page_id` / `end_page_id` for partial runs during iteration.
- **Artifacts under output** (typical layout; exact folder names may vary by backend, e.g. `auto/` vs `vlm/`):
  - `document_content_list.json` — **flat list of blocks in reading order**; entries include `type` (`text`, `image`, `table`, `equation`), `page_idx`, `bbox`, and for text optional **`text_level`** (documented example: `0` = body, `1+` = heading-like levels).
  - `document_middle.json` — **per-page** structure: `pdf_info[]` with `para_blocks`, `lines`, `spans`, geometry-oriented detail; good for debugging layout, heavier than needed for a first outline.
  - Also: `document.md`, optional layout PDFs, `images/`, etc.

**Implication for “top 3 layers”:** For the spike, the most direct mapping is **heading depth from `document_content_list.json`**: treat `text_level` 1, 2, and 3 as the three layers (or the first three distinct positive levels present in the file). If a PDF has no detected headings, the spike should still print a clear message (“no heading layers detected”) rather than failing silently.

**Open questions to resolve during the spike** (MinerU version / backend may change field names):

- Exact stability of `text_level` semantics across backends (`pipeline` vs `hybrid-auto-engine` vs VLM).
- Whether `content_list` or a Markdown heading pass is more reliable for “book-like” chapters on your sample PDFs.

## CLI integration (existing patterns)

Per `.cursor/rules/cli.mdc` and `cli/src/commands/interactiveSlashCommands.ts`:

- Add a **`InteractiveRunSlashCommand`** with `literal: '/read'`, required **`argument`** for the book path, and **`run(argument)`** returning `Promise<{ assistantMessage: string }>` if parsing is async (MinerU will be slow).
- Validate path: file exists, extension `.pdf` for spike scope; resolve **absolute path** or cwd-relative consistently and document it.
- **User-visible errors:** map subprocess / Python / MinerU failures through existing `userVisibleSlashCommandError` patterns (file not found, MinerU not installed, non-zero exit, timeout).
- **TTY:** Long runs may need a one-line “Reading …” assistant line before work, or a **stage** (`stageComponent`) if you need cancellation/progress; for spike, simplest is **one assistant message** after completion unless UX is unusable.

**Process boundary:** Implement MinerU invocation as **`spawn` of `python3`** (or configurable `DOUGHNUT_MINERU_PYTHON`) running a **small script** shipped next to the CLI or embedded as a string — avoid importing MinerU from TypeScript.

## Assumptions

- Developer / user has installed **`pip install mineru`** into the Python environment that the CLI will invoke (same interpreter as `python3` on `PATH`, unless overridden).
- Spike runs on **developer machines** first; CI may skip or gate tests that require MinerU + sample PDF.

## Phased spike plan (scenario-shaped milestones)

Each phase should end with a **demo** (command + sample PDF) and a short **note in this file** (what worked, what did not). Follow planning discipline: **one main outcome per phase**, observable from the CLI or a minimal script.

### Phase A — MinerU alone (no Doughnut CLI)

**Outcome:** A standalone Python script (or REPL snippet) that:

1. Accepts a PDF path and optional page range (keep small for fast iteration).
2. Calls MinerU’s `do_parse` with **`f_dump_middle_json`** / content list enabled as needed.
3. Loads `document_content_list.json` (or the path your run actually produces) and prints a **compact outline**: only blocks with `type === 'text'` and `text_level` in `{1,2,3}` (or first three distinct heading levels), in reading order, with page index.

**Kill / pivot criteria:** If heading levels are empty for representative PDFs, try fallback: first N `para_blocks` titles from `document_middle.json`, or first H1–H3 from generated Markdown — document which source wins for the spike.

### Phase B — Subprocess contract from Node

**Outcome:** TypeScript module that:

1. Writes or reuses the Phase A script.
2. Spawns Python, passes PDF path + temp output directory, waits with a **generous timeout** (spike-only constant).
3. Returns **trimmed string** (outline) or structured JSON string for the CLI to wrap.

**Tests (spike-appropriate):** Prefer **black-box** test only if you can avoid flakiness: e.g. mock subprocess to return fixture JSON and assert outline formatting; full MinerU test remains **manual** or opt-in env flag.

### Phase C — `/read <book path>` in interactive CLI

**Outcome:**

1. Register `readSlashCommand` in `interactiveSlashCommands`.
2. `doc`: usage `/read <path to pdf>`, description that mentions **local PDF**, **MinerU/Python required**, and **outline = top three heading layers** (or the exact definition you locked in Phase A).
3. `run(path)` executes Phase B and appends the outline as **`assistantMessage`** (truncate with “…” if longer than N characters for TTY safety — spike decision).

**Optional:** Cap pages in spike (`end_page_id`) so a 500-page PDF does not block the session; document in command help.

### Phase D — Spike retrospective (required)

**Outcome:** Update this section with bullets:

- **Keep:** e.g. subprocess approach, `text_level` mapping.
- **Discard:** e.g. middle.json path, specific backend.
- **Follow-up product phase:** only if Phase C feels worth shipping behind a flag or with clearer deps (Nix, Docker, remote service).

## Testing strategy (spike)

| Layer | Role |
|--------|------|
| **Manual** | 2–3 real PDFs (textbook with TOC, novel scan, two-column paper) + record time and output quality. |
| **Unit** | Outline formatting from **fixture JSON** (no MinerU in CI). |
| **E2E** | Defer unless `/read` becomes a committed feature; MinerU in CI is heavy. |

## Risks and mitigations

| Risk | Mitigation |
|------|------------|
| Large downloads / GPU expectations on first run | Document in command help; optional page cap; point to MinerU docs for backend choice. |
| `text_level` missing or inconsistent | Fallback hierarchy (Markdown headings → middle.json); explicit user message when empty. |
| Security (arbitrary path) | Spike: resolve path, reject `..` abuse if needed; no shell interpolation; later: align with product attachment model. |
| Repo policy (Python dep in Nix) | Spike assumes user-installed `pip`; **do not** add MinerU to flake until spike says “ship”. |

## References

- MinerU (Context7): `/opendatalab/mineru` — `do_parse`, output files, `document_content_list.json` / `document_middle.json`.
- Product context: `ongoing/book-reading-research-report.md` — layout extraction spectrum, PDF limitations.
- CLI slash commands: `cli/src/commands/interactiveSlashCommand.ts`, `cli/src/commands/interactiveSlashCommands.ts`, `.cursor/rules/cli.mdc`.
