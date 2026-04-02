# Spike: `/read <book path>` — PDF via MinerU + optional EPUB outline (top three layers)

Informal spike plan. **Do not treat as shipped product work** until outcomes are reviewed. Aligns with `ongoing/book-reading-research-report.md` (PDF structure is unreliable; model/layout pipelines are on the spectrum) and `.cursor/rules/planning.mdc` (small user-visible slices, spike = learn fast, then keep or discard).

## Spike goal

Answer: Can we use MinerU (Python) for PDF and a separate lightweight pass for EPUB, then surface a useful “top three layers” outline in the Doughnut CLI via `/read <book path>`? MinerU does not ingest EPUB; the Phase A script uses OPF spine + HTML `h1`–`h3` for `.epub`.

**Success for the spike** (pick any that apply after trying real PDFs):

- MinerU runs locally with documented prerequisites and produces JSON (or equivalent) without blocking the whole CLI on permanent model downloads in CI (acceptable to require first-time developer setup).
- We can **define “three layers” operationally** (see below) and print a **human-readable outline** in the interactive assistant scrollback.
- We know **failure modes**: scanned PDF, wrong language, missing headings, huge books (time/memory), GPU vs CPU.

**Non-goals for this spike**

- Persisting books in Doughnut, notebooks, or the backend.
- Perfect TOC fidelity or MinerU-native EPUB (no EPUB in MinerU; only a simple EPUB heading scrape in the Phase A script).
- Bundling Python/MinerU inside the Node CLI bundle (expect **subprocess** to the user’s Python).

## Research notes (Context7: MinerU)

Source: Context7 library `/opendatalab/mineru` (OpenDataLab), plus project output reference docs indexed there.

- **Role:** High-quality PDF → Markdown + structured JSON (“一站式” extraction), with layout, tables, formulas, OCR paths depending on backend.
- **Python API pattern:** `do_parse(...)` with `output_dir`, `pdf_file_names`, `pdf_bytes_list`, `p_lang_list`, `backend`, `parse_method`, flags such as `f_dump_md`, `f_dump_middle_json`, optional `start_page_id` / `end_page_id` for partial runs during iteration.
- **Artifacts under output** (typical layout; exact folder names may vary by backend, e.g. `auto/` vs `vlm/`). **MinerU 3.x (pipeline, `parse_method=auto`)** writes under `{output_dir}/{pdf_stem}/auto/`:
  - **`{pdf_stem}_content_list.json`** — flat list of blocks in reading order (upstream docs sometimes say `document_content_list.json`; the on-disk name is stem-prefixed). Entries include `type` (`text`, `image`, …), `page_idx`, `bbox`, and for headings-as-text optional **`text_level`** (`0` = body, `1+` = heading-like).
  - **`{pdf_stem}_middle.json`** — per-page `pdf_info[]` with `para_blocks`, `lines`, `spans` (same naming pattern vs `document_middle.json` in older docs).
  - Also: `{pdf_stem}.md`, optional layout PDFs, `images/`, etc.

**Implication for “top 3 layers” (PDF):** Use **`text_level` 1–3** on `type: text` blocks in `{stem}_content_list.json`, with **`{stem}_middle.json`** as fallback (`title` / `level`). If nothing matches, print a clear message rather than failing silently.

**EPUB (outside MinerU):** Outline = **`h1`–`h3` in linear spine order**; quality depends on publisher markup (many books use classes instead of heading tags).

**Open questions to resolve during the spike** (MinerU version / backend may change field names):

- Exact stability of `text_level` semantics across backends (`pipeline` vs `hybrid-auto-engine` vs VLM).
- Whether `content_list` or a Markdown heading pass is more reliable for “book-like” chapters on your sample PDFs.

## CLI integration (existing patterns)

Per `.cursor/rules/cli.mdc` and `cli/src/commands/interactiveSlashCommands.ts`:

- Add a **`InteractiveRunSlashCommand`** with `literal: '/read'`, required **`argument`** for the book path, and **`run(argument)`** returning `Promise<{ assistantMessage: string }>` if parsing is async (MinerU will be slow).
- Validate path: file exists; extensions **`.pdf`** (MinerU) and optionally **`.epub`** (spine/HTML headings in Phase A script); resolve **absolute path** or cwd-relative consistently and document it.
- **User-visible errors:** map subprocess / Python / MinerU failures through existing `userVisibleSlashCommandError` patterns (file not found, MinerU not installed, non-zero exit, timeout).
- **TTY:** Long runs may need a one-line “Reading …” assistant line before work, or a **stage** (`stageComponent`) if you need cancellation/progress; for spike, simplest is **one assistant message** after completion unless UX is unusable.

**Process boundary:** Implement MinerU invocation as **`spawn` of `python3`** (or configurable `DOUGHNUT_MINERU_PYTHON`) running a **small script** shipped next to the CLI or embedded as a string — avoid importing MinerU from TypeScript.

## Assumptions

- Developer / user has a Python env with **MinerU pipeline deps** (e.g. **`pip install 'mineru[pipeline]'`** — plain `mineru` alone is not enough for `backend="pipeline"`). Same interpreter as invoked by the CLI (e.g. `.venv-mineru`, or `python3` on `PATH`, unless overridden).
- Spike runs on **developer machines** first; CI may skip or gate tests that require MinerU + sample PDF.

## Phased spike plan (scenario-shaped milestones)

Each phase should end with a **demo** (command + sample PDF) and a short **note in this file** (what worked, what did not). Follow planning discipline: **one main outcome per phase**, observable from the CLI or a minimal script.

### Phase A — MinerU alone (no Doughnut CLI)

**Outcome:** A standalone Python script (or REPL snippet) that:

1. Accepts a **PDF or EPUB** path and optional page range for PDF (keep small for fast iteration). EPUB is handled outside MinerU (OPF spine + HTML `h1`–`h3`).
2. **PDF:** calls MinerU’s `do_parse` with **`f_dump_middle_json`** and content list enabled. **EPUB:** no `do_parse`; OPF spine + BeautifulSoup on spine XHTML.
3. **PDF:** loads **`{stem}_content_list.json`**, prints outline from `type === 'text'` and `text_level` in `{1,2,3}` in reading order (page index); fallback **`{stem}_middle.json`**. **EPUB:** `h1`–`h3` only, spine index in the label.

**Kill / pivot criteria:** If heading levels are empty for representative PDFs, try fallback: first N `para_blocks` titles from `document_middle.json`, or first H1–H3 from generated Markdown — document which source wins for the spike.

#### Phase A demo (done)

- **Script:** `minerui-spike/spike_mineru_phase_a_outline.py`
- **Command:**

```bash
CURSOR_DEV=true nix develop -c .venv-mineru/bin/python minerui-spike/spike_mineru_phase_a_outline.py \
  "$HOME/Downloads/tw_future _of_software_development_retreat_ key_takeaways.pdf" \
  --end-page 4
```

- **Deps:** Plain `pip install mineru` is not enough for `backend="pipeline"`; this machine needed **`.venv-mineru/bin/pip install 'mineru[pipeline]'`** (torch, transformers, onnxruntime, etc.). First run downloaded layout/OCR/table weights, then processed five pages in about 1–2 minutes on CPU.
- **Output file name (MinerU 3.0.5):** `{pdf_stem}_content_list.json` under `{output_dir}/{stem}/auto/`, not `document_content_list.json`.
- **Sample outline (PDF):** `content_list` worked; headings appeared as `type: text` with `text_level` 1–2 (no `middle.json` fallback needed). Example: L1 “The future of software engineering”; L2 sections such as “Executive summary”, “1. Where does the rigor go?” on early pages.
- **EPUB:** same wrapper, path to `.epub`; `--start-page` / `--end-page` are PDF-only (ignored for EPUB). No MinerU run, no model download. Example book: **`~/Documents/books/10xorg-odd-e.epub`**.

```bash
CURSOR_DEV=true nix develop -c .venv-mineru/bin/python minerui-spike/spike_mineru_phase_a_outline.py \
  "$HOME/Documents/books/10xorg-odd-e.epub"
```

### Phase B — Subprocess contract from Node

**Outcome:** TypeScript module that:

1. Writes or reuses the Phase A script.
2. Spawns Python, passes **book path** + temp output directory (PDF only), waits with a **generous timeout** (spike-only constant).
3. Returns **trimmed string** (outline) or structured JSON string for the CLI to wrap.

**Tests (spike-appropriate):** Prefer **black-box** test only if you can avoid flakiness: e.g. mock subprocess to return fixture JSON and assert outline formatting; full MinerU test remains **manual** or opt-in env flag.

#### Phase B demo (done)

- **TS:** `cli/src/commands/read/mineruOutlineSubprocess.ts` — `runMineruOutlineSubprocess({ bookPath, cwd?, … })` → `{ ok, outline, source, note? }` or `{ ok: false, error, exitCode? }`. Default timeout **30 minutes** (`MINERU_OUTLINE_DEFAULT_TIMEOUT_MS`). PDF: `mkdtemp` under OS tmp, passes `--output-dir` to the script, deletes dir after the run.
- **Python:** Phase A script accepts **`--json-result`** (single JSON object on stdout; stderr unchanged).
- **Env:** `DOUGHNUT_MINERU_PYTHON` (default `python3`), `DOUGHNUT_MINERU_OUTLINE_SCRIPT` (optional; else walk cwd ancestors for `minerui-spike/spike_mineru_phase_a_outline.py`).
- **Tests:** `cli/tests/mineruOutlineSubprocess.test.ts` (mocked `spawn`; no MinerU in CI).

```bash
CURSOR_DEV=true nix develop -c .venv-mineru/bin/python minerui-spike/spike_mineru_phase_a_outline.py \
  /path/to/book.pdf --end-page 2 --json-result
```

EPUB with `--json-result` (no temp output dir; same example file as Phase A):

```bash
CURSOR_DEV=true nix develop -c .venv-mineru/bin/python minerui-spike/spike_mineru_phase_a_outline.py \
  "$HOME/Documents/books/10xorg-odd-e.epub" --json-result
```

### Phase C — `/read <book path>` in interactive CLI

**Outcome:**

1. Register `readSlashCommand` in `interactiveSlashCommands`.
2. `doc`: usage `/read <path to book>`, description that mentions **local `.pdf` or `.epub`**, **MinerU + `mineru[pipeline]` (or equivalent) for PDF**, and **outline = top three layers** (MinerU `text_level` / `middle.json` for PDF; `h1`–`h3` spine pass for EPUB).
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
| **Manual** | 2–3 real PDFs (textbook with TOC, novel scan, two-column paper) + record time and output quality; for EPUB spine/`h1`–`h3` behavior e.g. **`~/Documents/books/10xorg-odd-e.epub`**. |
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

- MinerU (Context7): `/opendatalab/mineru` — `do_parse`, output files; on disk expect **`{stem}_content_list.json`** / **`{stem}_middle.json`** under `{stem}/auto/` (see Research notes above).
- Product context: `ongoing/book-reading-research-report.md` — layout extraction spectrum, PDF limitations.
- CLI slash commands: `cli/src/commands/interactiveSlashCommand.ts`, `cli/src/commands/interactiveSlashCommands.ts`, `.cursor/rules/cli.mdc`.
