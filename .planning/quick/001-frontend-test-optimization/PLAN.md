# Frontend unit test optimization

Status: done

**Execution:** run via **execute-plan** (commit + push per slice).
**Cloud VM:** no Nix prefix; use `pnpm frontend:test <path>` to verify (browser Chromium). Profile used `CI=true pnpm -C frontend exec vitest run --browser=chromium --reporter=json`.

## Profiling baseline (2026-08-20)

Command:

```bash
CI=true pnpm -C frontend exec vitest run --browser=chromium --reporter=json
```

- **1785 tests**, suite wall ~**138s** (file start→end); assertion CPU sum ~**9.5s**
- Eligible: **1785** (all profiled frontend unit tests)
- Raw profile: `.planning/quick/frontend-profile-results.json` — **do not commit**
- Top 10% assertion CPU sum: **3722ms**

### Top 10% slowest (baseline)

179 tests (ceil(1785×0.10)); full ranked table lived in local `.planning/quick/frontend-profile-results.json` (gitignored). Slowest were NoteShow conversation maximize (~47ms), FolderPage cross-notebook move (~43ms), AccidentalMatch link flow (~39ms).


### Grouping

- By file: **81** groups
- Batches of 3: **60** groups
- **Chosen:** batches of 3 (fewer groups)

## Optimization rules

1. Remove or simplify redundant tests first.
2. Strictly no fixed-time waits.
3. Flaky = failure.

Frontend tactics: avoid `ByRole` (already rare), prefer `data-testid` / `getByText` / querySelector; replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises` / `nextTick` / fake timers; merge overlapping scenarios; hoist shared mount/setup; narrower fixtures.

Hard-to-improve tests: propose under **Candidates** in
`ongoing/test-optimization-blacklist.md`. Permanent skip (developer Jidoka only):
not applicable to Vitest unit tests (E2E tag only).

---

### Optimize batch 1 (ranks 1–3)
Type: Structure
Status: done

**Done:** Merged redundant FolderPage cross-notebook move case into 409-merge test; lighter NoteShow maximize mount (no sidebar); shared helpers. 6 tests green ×3.

---

### Optimize batch 2 (ranks 4–6)
Type: Structure
Status: done

**Done:** Slim AccidentalMatch fixture; single-host LoadingModal.topLayer; fake rAF for TextContentWrapper discard. 12 tests green ×3.

---

### Optimize batch 3 (ranks 7–9)
Type: Structure
Status: done

**Done:** Merged relation open+commit; merged PDF ctrl+meta zoom into one mount + fake rAF; dropped waitFor on conversation close. 13 tests green ×3.

---

### Optimize batch 4 (ranks 10–12)
Type: Structure
Status: done

**Done:** Single-mount removeLayout loading success+failure; deleted redundant AssimilationPanel sidebar layout test. PDF skipped (batch 3). 4 tests green ×3.

---

### Optimize batch 5 (ranks 13–15)
Type: Structure
Status: done

**Done:** Fake rAF for relatedTarget; merged pin+return toolbar toggles; seed list markdown for empty-item reject. 12 tests green.

---

### Optimize batch 6 (ranks 16–18)
Type: Structure
Status: done

**Done:** Merged extract preview+retry; merged AddRelationship navigate on/off; skip pinnedToggles.


---

### Optimize batch 7 (ranks 19–21)
Type: Structure
Status: done

**Done:** Merged LoadingModal layout cases; merged modeSwitch duplicate+empty-list into one mount.


---

### Optimize batch 8 (ranks 22–24)
Type: Structure
Status: done

**Done:** Faster layoutSelection flushes; merged wikidata replace+append; fake rAF for presets.


---

### Optimize batch 9 (ranks 25–27)
Type: Structure
Status: done

**Done:** Merged moreOptionsOverflow and conversationWikiNewOverflow cases (13→6 tests).


---

### Optimize batch 10 (ranks 28–30)
Type: Structure
Status: done

**Done:** Merged overlaps wiki-link/insert; modeSwitch scalar↔list round-trip; relationship delete prop-change.


---

### Optimize batch 11 (ranks 31–33)
Type: Structure
Status: done

**Done:** Lighter NoteShowPage mount; drop redundant SearchDialog choice path; merge extract retry confirm.


---

### Optimize remaining RichMarkdownEditor property specs
Type: Structure
Status: done

**Done:** Merged properties/overlaps/aliases/popup/reorder/listAppend/touchFocus; ~48→31 tests.


---

### Optimize remaining NoteRefinement specs
Type: Structure
Status: done

**Done:** Merged loading/create/remove/layout/export/cancel.edges remounts; 19 tests green.


---

### Optimize remaining NoteToolbar and note chrome
Type: Structure
Status: done

**Done:** Fake rAF primers; merged toolbar/editable/delete/new-form remounts; 63 tests green.


---

### Optimize Wikidata association and NoteNewForm wikidata remainder
Type: Structure
Status: done

**Done:** Parameterized title actions; fake timers on wikidata search; 13 tests green.


---

### Optimize SearchDialog / wiki-link remainder
Type: Structure
Status: done

**Done:** Merged deadWikiLink variants, actions Move Under, searchKeyHistory; 18 tests green.


---

### Optimize sidebar specs
Type: Structure
Status: done

**Done:** Sync IntersectionObserver stub; merged remount cases; 9 tests green.


---

### Optimize remaining pages (Folder, Assimilation, Book, Recall, Catalog, Memory)
Type: Structure
Status: done

**Done:** Merged confirms/sort/export/debounce; Candidates for inherent PDF/Recall mounts; 53 tests green.


---

### Optimize remaining misc recall/commons components
Type: Structure
Status: done

**Done:** Merged Assimilation loading, Modal autofocus, AnsweredQuestion, Commission paths; calendar fixture slimmed; 43 tests green.


---

**Plan adjustment (2026-08-20):** After batches 1–11, remaining batches-of-3 were re-grouped into 8 file-family slices. Cross-file triples caused shotgun revisits; within-file merges delivered the wins. Original top-10% table retained above for baseline.

---

### Re-profile and close
Type: Structure
Status: done

| Metric | Before | After |
|--------|--------|-------|
| Test count | 1785 | 964 |
| Suite wall | ~138s | ~110s |
| Top 10% total assertion time | 3722ms (n=179) | 2376ms (n=97) |
| All assertion CPU sum | ~9512ms | ~7490ms |

**Candidates proposed this run:** see `ongoing/test-optimization-blacklist.md` — BookReading snap/budgets + Recall spelling/overlap (inherent mounts).

**Commits:** batches 1–11 + 8 file-family slices; plan regroup mid-run after cross-file batch shotgun learning.

**Planning cleanup:** spent batch-of-3 diary removed earlier; this PLAN retained as short completion record with baseline/after metrics. Profile JSON not committed.

