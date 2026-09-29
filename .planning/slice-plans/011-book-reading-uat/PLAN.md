# Find what blocks complete, smooth, stable book reading through a two-hour UAT

**Identity:** SEED-054#story-1
**Source:** [story](../../seeds/SEED-054-book-reading-uat.md#story-1), refined with the owner's
2026-09-29 decisions recorded there (agent-run UAT, internet books, real MinerU, real OpenAI).

## Goal and scope

The owner receives one findings report, a `## UAT Findings` section in the seed. It lists defects and
improvement opportunities in the book reading behavior supported today, with suggested priorities and
follow-up candidates. Precedent: SEED-042's sidebar UAT (commit `dbe4a996b4`).

Included: the story's starting scenarios (attach, browse, current block and selection, reading records
and resume, reorganizing by hand and with AI), smoothness, visual presentation, and stability across
1440×900, 1280×560, and 390×844 where practical.

Excluded: product code, automated tests, tooling changes, fixing anything found, creating backlog
entries or seeds for findings, and evaluating capabilities that do not exist (only noting where their
absence blocks a supported journey).

Budget: 120 minutes of exploration across slices 2 and 3 (about 50 + 55, plus a 15-minute reserve
kept for surprises and confirmation). Setup (slice 1), MinerU extraction waits, and write-up are timed
and reported separately.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| Real MinerU runs locally | `PYTHONPATH=/Users/terryyin/git/doughnut/.venv-mineru/lib/python3.12/site-packages /opt/homebrew/bin/python3.12 cli/python/mineru_book_outline.py <arxiv-attention.pdf> --json-result --output-dir <tmp>` | Exit 0 in 55 s (0.31 page/s) with a correct nested outline. The venv's own `bin/python3` is a dangling Nix store link, so the CLI needs `DONUT_MINERU_PYTHON=/opt/homebrew/bin/python3.12` plus that `PYTHONPATH` |
| Think Python extraction fits the CLI timeout | `pdfinfo thinkpython2.pdf`; `MINERU_OUTLINE_DEFAULT_TIMEOUT_MS` in `cli/src/commands/mineruOutline/mineruOutlineTypes.ts` | 244 pages ≈ 13 min at 0.31 page/s; timeout is 30 min |
| Books are available | `curl` downloads into `$CLAUDE_JOB_DIR/tmp/books`; `file *` | `thinkpython2.pdf`, `arxiv-attention.pdf`, `alice.epub`, `origin-of-species.epub` are valid PDF/EPUB |
| DRM refusal is triggered by `META-INF/encryption.xml` | Read `backend/.../services/book/EpubAttachValidator.java:119` | Yes; a derived copy with that entry exercises the refusal |
| Real OpenAI is reachable only from the Development stack | `OPENAI_API_TOKEN` is set in the shell and Nix shell; `.agents/agent-map.md` says E2E mocks OpenAI and linked worktrees refuse the Development stack | Development stack from the default checkout at `http://127.0.0.1:5175/`; not running now (`curl` → 000) |
| A browser can be driven headlessly | `ls node_modules/.pnpm \| grep playwright`; `~/Library/Caches/ms-playwright` | Playwright 1.63 with Chromium installed |
| The CLI can attach a PDF to a notebook | Read `cli/src/commands/notebook/notebookAttachSlashCommand.tsx`; `pnpm cli` in root `package.json` | Interactive CLI: `/set-access-token`, `/use <notebook>`, `/attach <path>`; `pnpm cli` targets port 8081, so the Development backend port must be confirmed in slice 1 |

Unobserved until slice 1 (probe): the Development stack starts and serves the app; the CLI connects to
it with an access token created in the web UI.

## Proof

This story changes no product code. Proof is the findings report's evidence: for each defect, expected
versus observed, reproduction steps, book and viewport, and screenshots or measurements kept under
`$CLAUDE_JOB_DIR/tmp/uat/` (not committed). Suspected but unreproduced problems are recorded with the
conditions tried, never as confirmed defects.

## Ordered slices

### 1. UAT environment is usable end to end
Type: Structure
Status: done
Size: about 15 minutes, outside the UAT budget.
Proof: the Development app serves the login page; a Playwright session signed in as the UAT account sees
its notebooks; the interactive CLI accepts the access token and `/use` of a `UAT …` notebook. Record
setup minutes. Explained empty change: nothing to commit besides this plan's status.

Probe: if the Development stack or CLI connection cannot be established, stop dependent slices and
revise this plan (fallback: E2E stack with AI reorganization recorded as a coverage gap).

Enables slice 2.

### 2. Attaching and browsing findings are recorded
Type: Behavior
Status: done
Size: about 50 minutes of UAT budget, plus MinerU waits.
Proof: `## UAT Findings` in the seed gains setup, books, and findings for attach, browse, current block,
and selection, each with evidence.

Behavior: real books and a signed-in reader → attach Think Python and the arXiv PDF with CLI `/attach`
(real MinerU), attach both EPUBs and the derived-DRM EPUB in the web UI, then browse PDF and EPUB layouts,
jump to deep blocks, scroll, compare current block with selection, return to the selection, and check the
layout stays scrolled to the current block at 1280×560 → observed defects and improvements are written.

### 3. Reading records and reorganizing findings are recorded
Type: Behavior
Status: done
Size: about 55 minutes of UAT budget plus the 15-minute reserve.
Proof: the same section gains findings for reading records, resume, manual reorganization, and AI
reorganization, each with evidence.

Behavior: attached books from slice 2 → mark blocks read and skimmed, observe heading-only auto-marking,
the control panel's target and the last block, leave and return to resume; indent/outdent with
descendants, cancel a block, create a block from a bbox with a typed title, run AI reorganization preview
and confirm; check the layout, current block, and records stay consistent → findings are written.

### 4. The owner can choose follow-up work from the report
Type: Behavior
Status: done
Size: about 10 minutes, outside the UAT budget.
Proof: the section ends with actual UAT time, scenarios not reached, suggested priorities, and proposed
follow-up story outcomes separating fixes to supported behavior from missing capabilities.

Behavior: findings from slices 2–3 → synthesize → the owner can select fixes and capabilities without
rereading the session.

## Current decisions

- Development stack from the default checkout (real OpenAI); UAT data in `doughnut_development` under
  notebooks named `UAT …`.
- MinerU via `DONUT_MINERU_PYTHON=/opt/homebrew/bin/python3.12` and
  `PYTHONPATH=/Users/terryyin/git/doughnut/.venv-mineru/lib/python3.12/site-packages`; repairing the
  owner's venv is out of scope, but its broken interpreter link is reported as a setup observation.
- Screenshots and raw measurements stay out of the repository; the report cites the retained measurements.

## Learnings

- Slice 1 (explained empty change, accepted 2026-09-29): Development stack healthy at commit `1f9bdae1bc`
  after deleting stale `backend/build/classes` in the default checkout (a removed
  `NotebookGitCutoverService` was still referenced). Account `manual`; notebook `UAT Think Python` is
  id 17; CLI access token `UAT CLI`. Reusable helpers live in `$CLAUDE_JOB_DIR/tmp/uat/`: `pw.js` /
  `shot.js` (Playwright with saved session), `cli.js` (CLI via node-pty with MinerU env), `attach.sh`
  (background `/use` + `/attach`, timed log). Setup: about 5 minutes for the probe plus about 10 minutes of
  stack and book preparation.
- The CLI `/attach` also accepts EPUB (raw upload), so both EPUB routes (CLI and web) are in scope for
  slice 2.
- Slice 2 (accepted 2026-09-29): exploration 07:33–08:00, 27 of 50 budget minutes; breadth complete, so
  93 of the 120 budget minutes remained for slice 3. Real MinerU took 167 s for Think Python
  (not ~13 min) and 28 s for the arXiv paper. Findings: nine defects and the improvements are in the
  seed's `## UAT Findings`. Evidence inspected: `shots/s2-11-tp-mobile-open.png` (defect 2) and
  `s2-tp-layout.txt` (defects 5 and 6). Notebooks for slice 3: UAT Think Python 17 (PDF book 5), UAT Alice
  18 (EPUB 3), UAT DRM Alice 19 (no book), UAT Origin of Species 20 (EPUB 4), UAT Attention 21 (PDF 6).
  Helper `br.js` gives reader goto, layout dump, and current/selected state.
- Slice 3 (accepted 2026-09-29): exploration 08:07–08:36, 29 minutes; breadth complete, reserve unused,
  so the UAT used 56 of 120 minutes. Defects 10–17 added; evidence inspected: `shots/s3-33-ai-err-17.png`
  and `s3-ai-tp.txt` (defect 10: AI reorganization of the 361-block Think Python returns HTTP 500 and shows
  truncated raw JSON). AI reorganization of the 27-block paper previewed in 3–6 s and was mostly correct.
- Slice 4 (accepted 2026-09-29): the report ends with time, suggested priorities (P1: defects 1, 2, 5, 6,
  10), 11 fix stories and 10 capability stories, and recommendations on the seed's Open Decisions. Three
  P3 polish improvements are intentionally in no follow-up story.

## Execution complete

Product advice: Queue the report's P1 fix stories first (EPUB chapter landing; PDF extraction headings and
nesting; AI reorganization on a real-size book; the phone-width reader), as one defect seed holding the
proposed fix stories, the way the sidebar UAT's findings were queued. Consider the small "change or clear a
reading record" capability early because the defects already leave wrong records. Before closing, the owner
decides whether to spend part of the 64 unused budget minutes on two gaps testable in the same setup (a
mid-size PDF to find where AI reorganization starts failing; EPUB heading-only auto-marking); the report's
sentence that the remaining gaps need other browsers, devices, or book sizes is only partly accurate.
