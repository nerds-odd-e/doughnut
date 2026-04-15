# EPUB support — research quest (parity + open questions)

**Purpose:** Inventory **what PDF book reading already does** so EPUB can be scoped for **feature parity**, and list **research topics** we must resolve before design — **without answering them here**.

**Sources:** [`book-reading-user-stories.md`](book-reading-user-stories.md), [`doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md), [`book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md), and current product/backend behavior (attach, storage, reader, reading position, reading records).

---

## 1. PDF book — features to treat as parity targets for EPUB

These are **user-visible or integrator-visible** capabilities the PDF path has (shipped or explicitly planned in the same stories), stated in **format-agnostic terms** where possible. EPUB work should decide which items are **in scope** for a first slice vs deferred.

### 1.1 Import and notebook binding

- Attach a book to **at most one** notebook per product rules, with **book metadata** (name, format, layout).
- **CLI** path: select notebook, attach **file + book layout** in one logical operation aligned with **`attach-book`** (multipart: JSON layout + bytes).
- **Server** persists **book record** and **blob** (production object store; local/test store in dev); **`GET …/book/file`** serves bytes for the reader.
- **Deletion** (frontend flow): remove book record and **delete stored object** from the configured backend.

### 1.2 Structure: book layout (`BookBlock` tree)

- **`GET …/book`** returns **flat preorder** blocks with **`depth`**, **`structuralTitle`**, **`layout_sequence`**, and navigation geometry (**`allBboxes`** as ordered **`PageBbox`** today).
- **Imported layout** from attach payload; server derives **first anchor** and related geometry (PDF: MinerU-derived **page + normalized bbox**).
- **Synthetic root block** (e.g. **`*beginning*`**) when structural import leaves leading body content before the first heading-like anchor.
- **Reorganization** (planned / partial per user stories): drag indent/outdent, keyboard depth, merge/cancel block, create block, AI-assisted depth — **tree editing** is largely format-agnostic but may depend on how **content blocks** and anchors exist for EPUB.

### 1.3 Imported content stream (`BookContentBlock`)

- Persist **raw imported** content items **per owning `BookBlock`** for **direct-content heuristics** and future reader features (PDF: MinerU **`content_list`** shape).
- **Direct content** definition: linear reading-order gap between consecutive **`BookBlock`** starts; **`hasDirectContent`** (and similar) driven by **typed** imported blocks (PDF: **`text` / `table` / `image`**, exclusions for headings, headers/footers, `page_*`, etc.).

### 1.4 Reader shell (main pane + book layout)

- **Main pane:** primary reading surface; **book layout** in **left** drawer/panel; responsive **default open/closed** by breakpoint; **toggle** without losing scroll/zoom where applicable.
- **GlobalBar** (or equivalent): book/notebook context, **book layout** control, format-specific controls (PDF: **zoom**, **page indicator**).
- **Accessibility:** distinguish **current selection** (user tapped a block) vs **current block** (viewport-derived); **live region** for meaningful title changes when appropriate.

### 1.5 Navigation sync (structure ↔ document)

- **Layout → document:** activating a **book block** scrolls/brings the **start** (and direct-content region semantics) into view with **safe-area / chrome** awareness.
- **Document → layout:** scroll/page changes update **highlighted current block** (debounced / hysteresis to avoid flicker); optional **scroll-into-view** for long layout lists.
- **Bad or missing anchors:** defined **no-op or fallback** behavior (PDF behavior exists; EPUB must define equivalents).

### 1.6 Gestures and viewport (PDF-specific today; EPUB needs equivalents)

- **Vertical reading scroll** as the default gesture; avoid trapping scroll in nested panes unnecessarily.
- **Zoom:** PDF uses **pdf.js** scale (ctrl/meta + wheel, pinch); EPUB needs a **clear zoom/reflow story** (may differ: reflow vs paginated vs fixed-layout).

### 1.7 Reading position (resume)

- **`GET` / `PATCH …/book/reading-position`:** persisted **cursor** (PDF: **page index**, **normalized vertical position**, optional **`selectedBookBlockId`**).
- **Restore on open:** reader resumes **last position** and optional **selection**.

### 1.8 Reading records (progress per block)

- **`GET …/book/reading-records`** and **`PUT …/book/blocks/{bookBlock}/reading-record`** with dispositions (**READ**, **SKIMMED**, **SKIPPED**).
- **Auto-mark** when **predecessor has no direct content** and user advances (PDF: shipped heuristic).
- **UI:** **Reading Control Panel** — geometry-gated anchoring vs bottom-docked, **read / skim / skip**, coexists with drawer and scroll-through rules.

### 1.9 In-reader creation from content (planned)

- **Long-press** on an **imported content block** in the reading stream → **new `BookBlock`** as child of owning block; **title dialog** when source text is long (see reorganize + UX roadmaps).

### 1.10 Testing and observability

- **E2E** coverage for **layout ↔ reader** sync, reading records, drawer behavior (PDF uses **canvas OCR** for some assertions — EPUB may need a **different** observable strategy).

### 1.11 Format identifier and API constraints

- Wire **`format`** (today **`pdf`** only enforced server-side for attach). EPUB will need **`epub`** (or similar) and **consistent validation** of file type vs declared format.

---

## 2. Open research topics (questions only — no answers in this doc)

Research should **not** be limited to the bullets below; they are **seeds**. Each item should eventually become a **decision** (product + architecture) with **evidence** (spike, prototype, or doc reference).

### 2.1 Extraction pipeline (your topic)

- Can we obtain **`BookBlock`-quality structure** and **`BookContentBlock`-quality stream** from EPUB **without** routing through the same **MinerU** (or MinerU-like) pipeline used for PDF?
- If **yes**, what is the **minimal** extraction pass (ZIP + OPF + spine + XHTML parsing only vs optional **epub.js** / **readium** parsing on server or client)?
- How do we handle **fixed-layout EPUB**, **mixed LTR/RTL**, **vertical writing**, and **heavy image-only** chapters relative to “content blocks”?
- **DRM or encrypted** EPUB: do we **explicitly exclude** them, or detect and fail fast with a clear error?

#### 2.1 Research findings (Apr 2026)

**MinerU not required for structure + body stream.** EPUB is a ZIP container: `META-INF/container.xml` → package document (OPF), **manifest** + **spine** define reading order, **XHTML** (and SVG) carry content. A **minimal server-side pass** can be: unzip → parse OPF → walk spine → parse each content document with an HTML/XML parser → derive:

- **Outline / `BookBlock` tree:** Prefer **EPUB Navigation Document** (`properties="nav"`) or legacy **NCX** for TOC when present; **fallback** to heading tags (`h1`–`h6`) in spine order (same spirit as `MineruContentListLayoutBuilder`: stack by level). Many trade books expose a reliable nav; some only have headings in body — product may need **both** sources merged with clear precedence.
- **`BookContentBlock` stream:** Map block elements to persisted rows analogous to MinerU: `text` (paragraphs, list items), `image` (`<img>` / `<figure>`), `table`, optional `discarded` types for sidebars — store **raw fragment or serialised subtree** in `rawData` like today. **No layout engine** (epub.js / Readium) is *required* for extraction; those libraries are for **rendering** and **locator resolution** (section 2.3 below), not for “what text/images exist in the package.”

**`epub.js` / Readium on the extraction path:** Optional. Useful if we later want **CFI** or renderer-aligned offsets without reimplementing EPUB CF rules; for **first extraction**, ZIP + OPF + spine + DOM parse is smaller and easier to test headlessly.

**Coupling to today’s `allBboxes`:** `BookBlockContentBboxes` only builds `PageBbox` when `rawData` contains **`page_idx` (int)** and **`bbox` (4 numbers, MinerU-normalised)**. Reflow EPUB has **no** natural MinerU page. For attach **without** backend changes, a **placeholder** is to set `page_idx` to **spine document index** and `bbox` to full “virtual page” `[0,0,1000,1000]` (or derived bands) so layout → reader jumps are **degraded but wired**; real EPUB navigation should move to **format-specific locators** (section 2.3 below). **Fixed-layout** EPUB (`rendition:layout` **pre-paginated** or `fixed-layout` metadata) is closer to “page + region” and may map to synthetic pages more honestly once dimensions are read from viewport meta.

**Fixed-layout / RTL / vertical / image-only:** Treat as **graded support**: detect `rendition:layout`, `page-progression-direction`, and `writing-mode` in CSS/package metadata; **fail or warn** in product when we cannot yet map to a reader/locator story. **Image-only** spine items still yield **no headings** — optional single synthetic block per document (like `*beginning*`) or skip with a clear “no structure” outcome. **Heavy figures** map to `image` content blocks; `hasDirectContent` logic may need EPUB-specific exclusions later (e.g. decorative images).

**DRM / encryption:** Treat **`META-INF/encryption.xml`** as a **strong signal** that resources may be unreadable without a DRM stack; **fail fast** with a user-visible error unless we explicitly integrate a licensed DRM SDK. **Password-protected ZIP** (unusual for consumer EPUB) is a separate check. Plain EPUB 3 **font obfuscation** also uses `encryption.xml` but leaves XHTML readable — distinguish by **algorithm** / `EncryptedData` if we need fewer false positives.

**Spike:** `scripts/epub-extraction-spike.mjs` — run `node scripts/epub-extraction-spike.mjs <book.epub>` (repo already has `adm-zip`). Outputs spine, coarse heading-derived outline, `rendition:layout` hint, `encryption.xml` flag, and notes on limitations (nav not merged in the spike).

### 2.2 Frontend viewing technology (your topic)

- What **component or library** renders EPUB in the **main pane** (e.g. **epub.js**, **Readium**, **foliate-js**, custom **iframe + spine** navigation) — and how does that choice interact with **Vue 3**, **mobile Safari**, **accessibility**, and **bundle size**?
- **Pagination vs scroll:** does the product **simulate pages** (CSS columns) or **continuous scroll** — and how does that choice affect **reading position** and **sync with `BookBlock`**?
- How do we render **user theme** (font, size, margins) without breaking **anchor** stability?

#### 2.2 Research findings (Apr 2026)

**Sources:** Context7 library IDs `/futurepress/epub.js`, `/readium/ts-toolkit`, `/johnfactotum/foliate-js`, `/vuejs/core` (queried via Context7); plus `npm view … dist.unpackedSize` for `epubjs`, `@readium/navigator`, `@readium/shared`.

**Candidate renderers (main pane)**

| Option | Shape | Vue3 fit | Notes |
|--------|--------|-----------|--------|
| **EPUB.js** | `ePub(url)` → `book.renderTo(elementId \| HTMLElement, { manager, flow, width, height, snap })` | Mount into a **single container** (`ref`); init in `onMounted`, tear down listeners/`book.destroy` in `onBeforeUnmount` | **Paginated** (`flow: "paginated"`) or **scrolled** (`flow: "scrolled"`). **Themes:** `rendition.themes.register` / `default` / `select` / `fontSize`. **Location:** `relocated` / CFI via `rendition.display(cfi)`. Mature examples; **medium** ecosystem maintenance risk (check license: BSD-style). |
| **Readium Web** (`@readium/navigator` + `@readium/shared`) | `new EpubNavigator(container, publication, listeners, …, initialPosition, { preferences, defaults })` → `load()` / `destroy()` | Same pattern: **DOM container ref**; **`await navigator.destroy()`** on unmount | **First-class locators** (`Locator`, progression, positions, text snippets). **Reader prefs** (`submitPreferences`, `EpubPreferences`: font, columns, colors, `scroll: false`, etc.). **Higher** alignment with EPUB / accessibility standards; **TypeScript-first**. Snippet count in Context7 is smaller than EPUB.js — expect more reading of upstream docs for edge cases. |
| **Foliate-js** | **`<foliate-view>`** custom element or low-level `EPUB` + paginator modules | Works as **native custom element** in template or `document.createElement` + mount; listen for `relocate` / `load` | **Modular** (parse without loading whole file); **`flow`** `paginated` \| `scrolled` on paginator; **CFI**, href, fraction, section index. Docs stress **CSP / XSS-hardening** for user EPUBs. README: pagination uses **CSS multi-column** (same *class* of approach as EPUB.js) with called-out **perf and CSS quirks**; bisection for visible range described as more accurate than EPUB.js. |

**“Custom iframe + spine”** remains valid if we want **maximum control** (sanitizer, no third-party rendition lifecycle): higher engineering cost, duplicate problems (pagination, RTL, FXL) the libraries already partially solve.

**Bundle size (npm `dist.unpackedSize`, indicative only)**

- `epubjs` — **~6.4 MB** unpacked.
- `@readium/navigator` — **~1.5 MB**; `@readium/shared` — **~0.84 MB** unpacked (plus whatever else the app pulls for loading publications).
- Foliate-js: **not measured** here; modular imports may tree-shake better than a monolithic `epubjs` install — verify with the actual import graph in Vite.

**Pagination vs continuous scroll (product)**

- **Continuous scroll** matches the **PDF reader UX** direction (vertical reading as default; layout ↔ document sync via scroll). EPUB.js `flow: "scrolled"` and Readium `scroll: true` (preferences) / scroll-boundary APIs align with **viewport-derived “current block”** (see §2.3 for how to map without `PageBbox`).
- **Paginated** (CSS columns / “page turns”) matches **print-like** EPUB expectations and can simplify **“page” chrome**, but **reading position** is still **logical** (CFI / progression), not a fixed PDF page index. Foliate and EPUB.js both expose **page-like** labels where the EPUB provides `page-list` metadata; many books lack it.
- **Recommendation for parity with Story 2 scrolling:** bias toward **scrolled** reflow for v1; treat **paginated** as a user preference or FXL-only path after spikes on **mobile Safari** scroll + nested overflow.

**Mobile Safari**

- All three approaches ultimately render **HTML inside controlled surfaces** (iframes / shadow roots / in-document). Expect to **spike on real iOS**: `-webkit-overflow-scrolling`, nested scroll containers, **100vh** / dynamic viewport, and **gesture conflicts** with the **Reading Control Panel** (same class of issues as PDF pinch-zoom; EPUB may trade pinch for **text sizing** instead).

**Accessibility**

- Readium’s toolkit is positioned for **standards-aligned** reading systems (selection, locators, preferences API in snippets). EPUB.js exposes **keyboard** navigation in examples (`rendition.on("keyup")`). Foliate-js: verify **screen reader** behavior with the custom element in a prototype. **Live region** / “current title” patterns from the PDF reader should be re-used at the **shell** level, not inside the renderer if possible.

**User theme (font, size, margins) vs anchor stability**

- **Reflow EPUB:** **pixel** or **normalized-Y** anchors **cannot** stay stable under font/margin changes; **§2.3** should own the contract (**CFI**, Readium `Locator`, or **fragment + progression**). **Logical** locators are designed to survive reflow; **re-open** after theme change may still require **scroll-to-locator** to restore the same *reading position*.
- **Theming APIs:** EPUB.js `rendition.themes.*`; Readium `submitPreferences`; Foliate `setStyles` / per-doc injection on `load`. All are compatible with **re-applying the last saved locator** after a theme commit.

**Vue3 integration pattern (no spike committed)**

- Use a **dedicated child component** with a **template ref** on the host `div`, **`onMounted`** to construct the navigator/rendition, **`onBeforeUnmount`** / **`onUnmounted`** to **`destroy()`**, remove listeners, and revoke blob URLs. Do **not** wrap library objects in `reactive()`; pass **plain callbacks** into the library. Matches Vue core guidance for **template refs** and lifecycle cleanup (Context7 `/vuejs/core`).

**Spike (when EPUB is prioritized):** Time-box a **Readium-first** vertical slice in `frontend/` (sample `.epub`, dev-only route or Storybook): **scroll mode**, **locator round-trip** after theme/font change, **iOS Safari** scroll + control panel, **Vite bundle**. Revisit the library choice only if that spike misses acceptance.

**Decision (Apr 2026):** **Readium Web** — packages from [`readium/ts-toolkit`](https://github.com/readium/ts-toolkit) (e.g. **`@readium/navigator`**, **`@readium/shared`**, plus whatever the publication loader requires) — for the **main-pane EPUB renderer**. EPUB.js and Foliate-js remain documented above as **fallback references** if Readium blocks on a must-have constraint.

### 2.3 Locators: “page + bbox” for EPUB (your topic)

- PDF **`PageBbox`** is **page index + normalized bbox** in **MinerU 0–1000** space. What is the **canonical EPUB locator** for the **same conceptual jobs**: block start, direct-content regions, **reading position**, future **citation** (`SourceSpan`)?
- Candidates to evaluate: **EPUB CFI**, **XPath + fragment**, **spine index + file offset**, **character offset in flattened text**, **CSS selector + range** — which are **stable** under **reflow**, **font change**, and **user stylesheet**?
- Do we **virtualize “pages”** for EPUB to reuse **`pageIndex` + `normalizedY`**, or **replace** reading-position schema for non-PDF formats?
- How do we implement **viewport → current block** when there is **no pixel bbox** (pure reflow HTML)?

#### 2.3 Research findings (Apr 2026)

**Sources:** [Readium Locator model](https://readium.org/architecture/models/locators), [HTML location extensions](https://readium.org/architecture/models/locators/extensions/html.html), [Best practices per format (EPUB)](https://readium.org/architecture/models/locators/best-practices/format.html); EPUB CFI spec (IDPF); public discussion of **renderer timing** after theme changes (e.g. epub.js issues — **implementation** caveats, not a rejection of CFI as a logical identifier).

**Canonical on-the-wire shape (aligns with architecture roadmap + §2.2 Readium choice):** treat **[Readium `Locator`](https://readium.org/architecture/models/locators)** (JSON-serializable: `href`, `type`, optional `title`, `locations`, `text`) as the **format-native** replacement for PDF **`PageBbox`** for **EPUB reflow**. It is the same *conceptual* object as “where in the publication,” not a pixel rectangle.

| Conceptual job (parity with PDF path) | PDF today | EPUB reflow (recommended) | EPUB fixed-layout (pre-paginated) |
|--------|-----------|---------------------------|-------------------------------------|
| **Block start** (layout → document) | `allBboxes[0]` (page + norm. bbox) | **`Locator`** with `href` + **precise** `locations.partialCfi` and/or **`fragments`** (`#id` when the author spine item has a stable id) + optional `locations.progression` | Same **logical** locator **or** geometry closer to PDF (see below) |
| **Direct-content extent** | Further `PageBbox` entries / gap heuristics | **Derived** between consecutive block-start locators in reading order (same *idea* as architecture “gap”), backed by imported **`BookContentBlock`** stream; optional **ranges** for UI | DOM/region-based equivalents where layout is page-like |
| **Reading position (resume)** | `pageIndex` + `normalizedY` + optional `selectedBookBlockId` | **`Locator`** with EPUB **minimum** fields per Readium: `href`, `type`, `locations.progression`; **should** add `partialCfi` / selector / range + `text` for robust re-entry (best practices) | Prefer **locator +** optional **page-like** fragment if the toolkit exposes it; still avoid MinerU-style norm bbox unless we deliberately map a **viewport rect** |
| **Citation (`SourceSpan`)** | TBD; likely page + view rect | Readium **requires** one of `partialCfi`, `cssSelector`, or `domRange` for highlights/annotations **plus** `text` (per format best practices) | Same; fixed-layout may add **rect** fragments where the publication model allows |

**Candidate mechanisms — stability (reflow, font size, user theme):**

- **`locations.progression` / `totalProgression` / `position`:** **Coarse but robust** under reflow and theme; good for **approximate** resume and progress bars; **not** enough alone for precise citations or tight “jump to this paragraph.”
- **`partialCfi` (EPUB CFI fragment side):** **Logical** pointer into the XHTML DOM (character / structural path). Intended to survive **reflow** because it is **not** tied to pixels. **Caveat:** consuming code must **re-resolve after layout** (fonts loaded, column/scroll reflow finished); some open-source viewers had **bugs** where `display(cfi)` ran before layout settled — product should follow Readium’s lifecycle (`relocate` / preferences applied) and re-apply the saved locator if needed.
- **`href` + HTML `id` fragment (`fragments`):** **Stable** when the author’s markup includes persistent ids; **weak** when headings lack ids (then generate at import only if we **inject** or **pin** anchors deterministically and never rewrite them).
- **`cssSelector` / serialized `domRange`:** **Powerful** for highlights; **sensitive** to **sanitization** and DOM normalization differences between extraction and renderer. Prefer **CFI** or **injected ids** for long-lived stored locators unless we control DOM identity end-to-end.
- **Spine index + byte offset / plain “character index in flattened text”:** **Poor** for reflow EPUB — **avoid** as a canonical stored locator.
- **XPath (non-standard in Readium locator):** Same fragility as selectors; **not** the first choice for interchange.

**Virtual “pages” vs new schema:** For **reflow EPUB**, **do not** treat `pageIndex` + `normalizedY` as the **canonical** reading cursor (pixels move when font or margins change). Keep today’s columns for **PDF**; add a **parallel** representation for EPUB (e.g. JSON **`Locator`** on **`PATCH …/reading-position`**, or a nullable `epubLocator` / `formatSpecific` field) rather than overloading MinerU semantics. **Fixed-layout / pre-paginated** EPUB is the exception: **synthetic “page index” + region** (and even **normalized bbox** in a chosen coordinate space) can be **honest** because spread geometry is meaningful — closer to Readium’s PDF-style `page` + `viewrect` fragments than to reflow **`PageBbox`**.

**Viewport → current block without pixel bbox:** Reuse the **same product idea** as PDF (single **current block** from the viewport), but the **implementation** is **DOM- and locator-based**: after the renderer lays out the spine item, resolve each **`BookBlock`**’s **start anchor** to an **element or range**, then choose the block whose start (or “reading progression”) best matches the visible viewport — e.g. **largest visible fraction**, **scroll anchor**, or **comparison of `progression`** to block boundaries. **Reading Control Panel** “geometry gating” (§UX roadmap) generalizes to **“bottom of the direct-content range in document order is above the panel obstruction zone”** using **layout boxes from the live DOM** (not precomputed MinerU bboxes).

**Spike:** No separate standalone script is **required** for §2.3 if the **Readium** time-box in §2.2 runs: that spike should include **`relocate` → serialize `Locator` → `submitPreferences` (font) → re-open same `Locator`** and document any ordering workarounds. Optional **add-on:** during §2.1-style extraction, persist **per-block** `href` + first heading **`id`** or **computed `partialCfi`** so layout navigation does not depend on a second client-side inference pass.

### 2.4 Data model and API

- Should **`BookContentBlock`** rows **reuse** the same persistence shape for EPUB (type, order, optional **pageIndex**/bbox analog, raw payload), or **introduce** a parallel/import-specific table?
- Should **`allBboxes`** remain the **wire format** for EPUB, or should **`PageBbox`** be generalized (e.g. **union type**: PDF bbox vs EPUB CFI range)?
- **`GET …/book/file`:** serve **raw `.epub`** only, or also **expanded** artifacts (security + caching implications)?

#### 2.4 Research findings (Apr 2026)

**Sources:** current backend entities and services (`BookContentBlock`, `BookBlockContentBboxes`, `BookBlockDirectContentPredicate`, `NotebookBooksController#getBookFile`), OpenAPI `BookBlock_Full` / `PageBbox_Full`, plus §2.1 (extraction shape) and §2.3 (Readium `Locator`).

**`BookContentBlock` — reuse vs parallel table**

- **Decision:** **Reuse** the existing **`book_content_block`** row shape for EPUB imports. The table already carries **`type`**, **`sibling_order`**, optional **`page_idx`**, and **`raw_data` (LONGTEXT)** — enough to persist an ordered imported stream analogous to MinerU **`content_list`** (EPUB: paragraph / image / table / etc.) with **format-specific JSON** in `rawData` (e.g. spine **`href`**, fragment **`#id`**, optional **`partialCfi`**, excerpt text for titles, serialized subtree if needed).
- **Rationale:** One persisted concept — “imported body items owned by a **`BookBlock`**” — matches the architecture roadmap; avoids duplicating repositories, attach merge logic, and **`fromBookContentBlockId`** flows. A parallel table would split the same product concept across two code paths.
- **Implementation note:** **`BookBlockDirectContentPredicate`** and **`BookBlockContentBboxes.fromRaw`** are **MinerU-oriented** today (e.g. `text` + `text_level`, `page_idx` + four-number **`bbox`**). EPUB needs **either** branching on **`Book.format`** (or equivalent) **or** EPUB-specific types / raw keys so direct-content and **`allBboxes`** derivation stay correct without pretending every book is PDF.

**`allBboxes` / `PageBbox` on the wire**

- **Decision (reflow EPUB):** **Do not** treat **`PageBbox`** (page index + normalized rectangle) as the **canonical** navigation contract — align with §2.3: surface **Readium-shaped `Locator` JSON** (or a thin Doughnut wrapper with **`href`**, **`type`**, **`locations`**, **`text`**) for block starts and, later, reading position / citations.
- **Wire shape options (product + OpenAPI):**
  - **Preferred:** Add **parallel** fields on each **`BookBlock`** (e.g. **`blockStartLocator`**, optional list of **direct-content boundary locators**, or **`navigationAnchors: Locator[]`**) while keeping **`allBboxes`** for PDF-only semantics; clients branch on **`book.format`**.
  - **Alternative:** Replace **`allBboxes: PageBbox[]`** with a **discriminated union** (e.g. **`kind: "pdf"`** vs **`kind: "epub"`**) so one array name stays stable — slightly more coupling in the generator and frontend.
- **Interim (no schema migration):** The §2.1 **placeholder** (spine index as **`page_idx`**, full-band **`bbox`**) still produces **`PageBbox`** entries via existing **`fromRaw`** rules so **layout → reader** can be wired **degraded**; ship-quality EPUB should still add **real locators** so reflow does not depend on fake boxes.

**`GET …/book/file`**

- **Decision:** Serve **one canonical blob per book** from object storage — **raw `.epub`** for EPUB (media type **`application/epub+zip`**, same **private + ETag** caching pattern as PDF today). **Do not** expose a **stable public API** for **server-expanded** packages (unzipped OPF/spine tree) as the default: larger surface for **path/confusion bugs**, extra **cache invalidation**, and **no** need for Readium if the client can fetch the archive and open it locally.
- **Processing:** **Unpack / parse** during **attach** (or a dedicated internal job) only; persisted **`BookBlock`** / **`BookContentBlock`** rows are the **durable** derived data. If a future optimization caches expanded bytes privately, keep it **internal** to storage/workers, not **`GET …/book/file`** variants, unless product explicitly needs range requests on individual XHTML (then design **separate**, authenticated, sanitized resource endpoints — out of scope for first slice).

**Spike:** Not required for §2.4 alone — **§2.1** (`scripts/epub-extraction-spike.mjs`) and the **§2.2 Readium** slice cover extraction + locator round-trip; §2.4 is **schema/API policy** grounded in existing code paths.

### 2.5 CLI and attach UX

- **`/attach`**: same command with **format sniffing**, or **`/attach-epub`**? How does the CLI **run extraction** (local Node script, call backend pre-process, or upload-only)?

#### 2.5 Research findings (Apr 2026)

**Current behavior (PDF):** Interactive **`/attach`** reads a **`.pdf`**, runs **`runMineruOutlineSubprocess`** (Python **`cli/python/mineru_book_outline.py`** → MinerU `content_list` or layout), then **`attachNotebookBookWithPdf`** posts **multipart** `metadata` (JSON: `bookName`, `format: "pdf"`, `contentList` *or* `layout`) + **`file`** to **`POST /api/notebooks/{id}/attach-book`** (`cli/src/backendApi/doughnutBackendClient.ts`, `cli/src/commands/notebook/notebookAttachSlashCommand.tsx`). The CLI **rejects non-`.pdf`** today even though the same Python script documents **`.epub`** support (heading walk → `layout.roots`).

**If EPUB extraction is server-side (aligned with §2.1 / §2.4):** The CLI should stay **one user-facing command** — extend **`/attach <path>`** with **extension-based routing** (`.pdf` vs `.epub`), not a separate **`/attach-epub`**, unless product later needs divergent UX (unlikely). For EPUB, the CLI becomes **upload-centric**: read bytes → multipart POST with **`format: "epub"`** and **`application/epub+zip`** (or octet-stream + magic-byte check server-side) — **no local extraction** for EPUB once the backend accepts **`metadata` without `layout`/`contentList`** and derives blocks from stored bytes (or a dedicated **`POST …/attach-epub`** that omits precomputed layout; still one CLI command can call it).

**Avoid** keeping **Python EPUB outline** on the CLI **and** Java extraction on the server long term — two implementations of the same book structure will drift; keep Python for **PDF/MinerU** only unless EPUB stays CLI-only temporarily.

**Frontend parity:** “Attach book” in the UI is the **same multipart contract** as the CLI (file + metadata). Today there is **no** first-class EPUB attach in product code paths until **`AttachBookRequest`** / **`BookService`** allow **`epub`** and optional server-derived layout; the **main incremental surface** for users who do not use the CLI is **file input + same API**.

### 2.6 Server vs client work split

- Is **unpack + parse + normalize** done **server-side** (consistent with MinerU for PDF) or **client-side** (privacy, cost)? Implications for **tests**, **repeatability**, and **large files**.

#### 2.6 Research findings (Apr 2026)

**Decision (EPUB):** Do **unpack + parse + normalize on the server** during attach (same *phase* as persisting the blob), analogous to how the server is the **system of record** for stored PDF bytes and merged layout — but **unlike PDF**, do **not** depend on a **client-side** extractor for EPUB: MinerU does not eat EPUB, and a **ZIP + OPF + spine + XHTML** pass is cheap to run in Java (or a small internal library), test headlessly, and keep **one** canonical layout/content stream.

| Concern | Server-side EPUB | Client-side EPUB extraction |
|--------|------------------|-----------------------------|
| **Repeatability** | Same bytes → same persisted `BookBlock` / `BookContentBlock` rows | Depends on browser/CLI/runtime and shipped parser versions |
| **Tests** | Controller/service tests + fixtures; no MinerU | Extra E2E/CLI matrix; harder to assert parity with web upload |
| **Large files** | Same multipart limits / streaming policy as PDF attach; memory bounds explicit in service | User device parses full ZIP before upload; duplicate work if they then upload the same file |
| **Privacy** | File already uploaded for reading; extraction where the blob lives avoids shipping structure twice | Theoretical gain only if we ever supported “parse locally, never upload full EPUB” — **not** the current product model |

**PDF stays a split brain by necessity:** **MinerU** is intentionally run **locally** in the CLI (heavy, Python env); the server **merges** `contentList`/`layout` + stores the PDF. **EPUB has no such constraint** — prefer **server-only** extraction for structure and `BookContentBlock` stream.

**Spike:** Not required beyond **`scripts/epub-extraction-spike.mjs`** (§2.1) and eventual **Java** attach integration; no separate CLI spike unless prototyping **upload-only** attach before backend lands.

### 2.7 Parity gaps that are PDF-specific today

- **Pinch/zoom vs reflow:** what is EPUB’s equivalent of **pdf.js `currentScale`** for **reading position** and **control panel geometry**?
- **“Content block” hit targets:** PDF uses **overlay / MinerU geometry** for long-press; what is the EPUB **equivalent** (DOM range, paragraph id, CFI span)?
- **E2E:** what replaces **canvas OCR** for **deterministic** assertions?

#### 2.7 Research findings (Apr 2026)

**Sources:** shipped PDF reader (`PdfBookViewer.vue`, `pdfViewerViewportTopYDown.ts`, `bookBlockSelectionBboxHighlight.ts`, `BookReadingContent.vue`), E2E page object `e2e_test/start/pageObjects/bookReadingPage.ts`, plus §2.2 (Readium) and §2.3 (Locator).

**Pinch/zoom vs reflow — reading position**

- **PDF today:** Resume uses **`pageIndex` + `normalizedY`** (MinerU **0–1000** vertical coordinate on the **anchor page**). The client derives this from **live layout**: `pdfViewerReadingPositionTopEdge` maps the scroll container’s top edge into that space using each page view’s **`getBoundingClientRect`** / viewport height (`frontend/src/lib/book-reading/pdfViewerViewportTopYDown.ts`). **Gesture zoom** mutates **`pdfViewer.currentScale`** and adjusts scroll so the focal point stays stable (`applyGestureScaleFactor` in `PdfBookViewer.vue`); viewport sampling runs again and **patches** reading position when the anchor/midpoint changes.
- **EPUB parity:** There is **no** `currentScale` analogue that preserves a **pixel-normalized** Y on a fixed page grid. **Reflow** “zoom” is **font size / margins** (Readium preferences or CSS), which **moves** where text sits on screen. **Canonical resume** must be a **logical locator** (Readium **`Locator`**, §2.3) — **not** overload `pageIndex`/`normalizedY` for reflow EPUB. After a theme/font change, **re-`goTo`** the saved locator (same pattern as §2.2: serialize → change prefs → restore).
- **Fixed-layout EPUB** is closer to PDF: a **spread/page** + **region** (or toolkit “page + viewrect”) can back **geometry-like** resume if product wants it; still prefer **`Locator`** on the wire for one API shape where possible.

**Pinch/zoom vs reflow — Reading Control Panel geometry**

- **PDF today:** “Content-anchored” panel placement compares the **last direct-content bbox bottom** in **client coordinates** to a **bottom obstruction band** (`isLastContentBottomVisible`, `readingPanelAnchorTopPx` in `PdfBookViewer.vue`), using **page div** rects and MinerU **y1/1000** of the bbox.
- **EPUB parity:** Same **product rule**, different **measurement**: resolve the **DOM range or element** that corresponds to the end of direct content for the **selected** block (from imported stream + block boundaries or §2.3 boundary locators), then compare **`getBoundingClientRect().bottom`** to the same **obstruction** inset. No MinerU rectangle required — **live DOM boxes** after reflow. If the end cannot be resolved (bad anchor), fall back to **bottom-docked** panel (already in UX roadmap for PDF).

**“Content block” hit targets (long-press → new block)**

- **PDF today:** For selection highlights tied to **`BookContentBlock`**, the viewer appends **absolutely positioned overlays** on the pdf.js **page layer** with **`pointerEvents: auto`**, **`data-book-content-block-id`**, and a **500ms** hold with **10px** move tolerance (`bookBlockSelectionBboxHighlight.ts`). Coordinates come from **MinerU bbox** × **page viewport** (`normalizedBboxToPixelRect`).
- **EPUB parity options (pick one for v1):**
  1. **DOM attribution:** During render or post-process, tag elements that correspond to a persisted **`BookContentBlock`** (e.g. **`data-doughnut-content-block-id`** on the block’s root element). Long-press uses the **same** timer/tolerance pattern on those nodes (or thin transparent overlays aligned to their boxes if the DOM is not stable).
  2. **Pointer → locator → id:** Use Readium’s **selection / hit-testing** to map `(x, y)` to a **CFI/range**, then map CFI back to the owning **`BookContentBlock`** via import-time stored **`partialCfi` / href+fragment** (heavier, but no DOM decoration if we control extraction alignment).
  3. **Paragraph overlay layer:** Similar to PDF — draw hit rects from import geometry — but for reflow EPUB, rects must be **recomputed after layout** (ResizeObserver / relocate), so (1) is usually simpler.

**E2E — replacing canvas OCR**

- **PDF today:** Assertions use **Tesseract** on a **screenshot of the pdf viewer** (`expectVisibleOCRContains` in `bookReadingPage.ts`) because the reading surface is **canvas**, not selectable text.
- **EPUB parity:** Prefer **`cy.contains` / `should('include.text')`** on the **reader container** or on elements with **stable test hooks** (fixture EPUB with known chapter titles). **No OCR** needed for a normal HTML/text rendering path.
- **Caveats:** If Readium (or another renderer) puts content inside **shadow roots**, Cypress may need **`includeShadowDom: true`** or **custom commands** to pierce the shadow tree — **time-box in the §2.2 Readium spike** (single scenario: “visible passage X after navigate”).
- **Hybrid:** Keep **layout-side** assertions (`data-current-block`, `data-testid` on book blocks) identical to PDF tests; only the **“text is visible in main pane”** strategy switches from OCR to DOM text.

**Spike:** **Not a separate repo script.** Validate E2E strategy inside the **Readium vertical slice** (§2.2): one Cypress scenario on a **tiny fixture EPUB** proving **DOM text assertion** + **layout highlight** without Tesseract. If shadow DOM blocks queries, record the **minimal** pierce/config fix in the spike notes.

### 2.8 Product edge cases

- **Footnotes, endnotes, sidebars:** are they **in stream** or **hyperlinked** — how does that affect **direct content** and **reading order**?
- **Tables, code blocks, images:** mapping to **`BookContentBlock` types** and **hasDirectContent**.
- **Multiple renditions** in one EPUB (if present): which one is **canonical**?

### 2.9 Legal, storage, and ops

- **Licensing** of third-party EPUB renderers; **CSP** if **iframe** or **inline HTML**; **sanitization** of XHTML to prevent XSS when rendering user-provided EPUBs.

#### 2.8 Research findings (Apr 2026)

**Sources:** [DAISY KB — Reading order / spine / `linear`](https://kb.daisy.org/publishing/docs/epub/reading-order.html), [IDPF a11y — Notes (`noteref`, `footnote`, `endnote`, `aside`)](https://idpf.github.io/a11y-guidelines/content/xhtml/notes.html), [W3C — EPUB Multiple-Rendition Publications 1.1](https://www.w3.org/TR/epub-multi-rend-11/) (note); current backend `BookBlockDirectContentPredicate` (MinerU-oriented defaults).

**Footnotes, endnotes, sidebars — in stream vs linked**

- **Markup (typical EPUB 3):** References use `<a epub:type="noteref" href="…#id">`; note bodies often live in `<aside epub:type="footnote">` / `endnote` / `rearnote` (same document or another spine item). **Sidebars** often use `<aside epub:type="sidebar">` or class-based patterns.
- **Spine vs DOM:** Our **imported stream** and **direct-content gaps** (architecture roadmap) are defined along the **same linear path we use for structure**—by default **spine order** within each XHTML document’s **DOM reading order**. Footnote **asides** may appear **inline in the DOM** after the referencing paragraph (pop-up behavior is reading-system UI, not a guarantee we skip them in extraction).
- **Linked-only notes:** If the note body is **only** reachable via `href` and lives in a **separate** spine item (e.g. endnotes chapter), that item still appears in **spine sequence** unless `linear="no"`. **Non-linear** spine items (`linear="no"`) are **not reliably skipped** while paging; DAISY explicitly warns behavior varies by reading system.
- **Product impact:** For **v1 extraction**, pick explicit rules and document them:
  - **Option A (simpler):** Emit **`BookContentBlock`** rows for footnote/sidebar **asides** with a dedicated **`type`** (e.g. `note`, `sidebar`) and extend **`BookBlockDirectContentPredicate`** for **`Book.format == epub`** to **exclude** those types from **direct content** (parallel to ignoring `header` / `footer` / `page_*` for PDF), so reading-order heuristics match “primary narrative” rather than boilerplate notes.
  - **Option B:** Include notes in the stream as normal **`text`** and accept that **hasDirectContent** / panel geometry may treat notes as part of the gap (usually **noisier**).
- **Reading order vs TOC:** Merged nav + headings (§2.1) still defines **book blocks**; footnotes rarely become their own **book blocks** unless we add special casing. **No spike-only dependency**—rules belong in the **Java extraction** design when EPUB attach lands.

**Tables, code blocks, images — mapping and `hasDirectContent`**

| EPUB / HTML pattern | Suggested `BookContentBlock.type` | `contributesDirectContent` (today’s Java) |
|---------------------|-----------------------------------|-------------------------------------------|
| `<table>` (and EPUB table vocab where used) | `table` | **Yes** (not excluded). |
| `<img>`, `<figure>` with image | `image` | **Yes**. |
| `<pre>`, `<code>` blocks | **`text`** with EPUB-specific **`rawData`** (e.g. `epub_kind: "code"`) **or** a dedicated `code` type | If `code` is a **new** type: **Yes** (predicate only excludes `header`, `footer`, `page_*`, and heading-like **`text`** with `text_level` 1–3). If stored as **`text`**: **Yes** unless `text_level` is wrongly set. |
| Decorative / very small images | Product may later add EPUB **`role="presentation"`** / heuristic exclusions (similar to future MinerU image rules). | TBD in extraction. |

- **Alignment:** Reusing **`BookContentBlock`** (§2.4) means EPUB extraction should **populate `type` + `rawData`** so the **same predicate** can be extended with **`format`-aware** branches rather than duplicating logic.

**Multiple renditions in one EPUB**

- **Spec:** [EPUB Multiple-Rendition 1.1](https://www.w3.org/TR/epub-multi-rend-11/) describes **`container.xml`** with multiple **`rootfile`** entries, optional **`META-INF/metadata.xml`**, and rendition selection by metadata (e.g. `rendition:layout`, `rendition:accessMode`). **Practical prevalence:** low in typical consumer files; most EPUBs are a **single** package document.
- **Decision (v1):** Treat the **first** `rootfile` in **`META-INF/container.xml`** as the **canonical rendition** for attach + reader. If **more than one** `rootfile` is present, **log a structured warning** (ops) and still use the first unless/until product adds **explicit rendition choice** (e.g. prefer reflow over pre-paginated via `rendition:layout`).
- **Spike:** **Not required** beyond a **fixture check** when Java extraction exists (multi-rootfile sample in tests).

#### 2.9 Research findings (Apr 2026)

**Sources:** [Readium `ts-toolkit` LICENSE](https://raw.githubusercontent.com/readium/ts-toolkit/develop/LICENSE) (BSD-3-Clause); [Snyk / CVE-2021-33040 (epub.js XSS)](https://security.snyk.io/vuln/SNYK-JS-EPUBJS-2342194); [epub.js security discussion](https://github.com/futurepress/epub.js/issues/987); repo scan: no **Content-Security-Policy** string in `frontend/` (app may still set CSP at **edge / LB / hosting**).

**Licensing (renderers referenced in §2.2)**

| Component | License | Notes |
|-----------|---------|--------|
| **Readium `ts-toolkit`** (`@readium/navigator`, `@readium/shared`, etc.) | **BSD-3-Clause** (Readium Foundation) | Permissive; retain copyright notice in distributions as required. |
| **EPUB.js** | BSD-style (see npm `LICENSE`) | Permissive; track **security advisories** if ever used. |
| **Foliate-js** | **MIT** (library repo) | Distinct from the **GPL** **Foliate** *desktop app*; do not conflate the two repos. |

- **Ops:** Record third-party **NOTICE** / attribution in whatever mechanism the repo already uses for other BSD/MIT deps (no special copyleft obligations for the above).

**CSP (iframe / inline HTML / blob URLs)**

- EPUB content is **untrusted HTML/CSS** loaded inside the **reading surface** (iframe and/or shadow DOM, depending on toolkit). **CSP** interacts with: **`script-src`** (should block or omit user scripts for default trust model), **`style-src`** (`'unsafe-inline'` is often required for EPUB author styles unless rewritten), **`img-src`** / **`font-src`** (blob/data for embedded fonts and images).
- **Deployment:** If the **hosting** stack adds a **strict global CSP**, the EPUB reader route may need **route-specific relaxation** or **nonce/hash** strategies only where unavoidable—**this must be validated in the §2.2 Readium integration spike** (one EPUB with embedded font + inline styles).
- **Principle:** Prefer **default no scripted EPUBs**; align with reading-system practice (scripts off unless explicit trusted mode).

**Sanitization and XSS**

- **Threat:** Crafted EPUB XHTML with **`<script>`**, inline **event handlers**, **`javascript:`** URLs, or **SVG** script can lead to XSS if the renderer executes or merges content into the host origin without isolation.
- **Defense in depth:**
  1. **Renderer / toolkit policy:** Use APIs that **disable scripting** for user uploads; verify **Readium**’s defaults and toggles in the spike.
  2. **Sandboxed iframe** where applicable (epub.js historically uses **`sandbox`** with limited flags; **`allow-scripts` + `allow-same-origin`** together weakens isolation—avoid for untrusted content).
  3. **Server-side sanitization** of extracted fragments if we ever **echo** XHTML outside the reader or **share** HTML with other surfaces; for **Readium-only** display, prioritization is **toolkit + CSP + no script**.
- **Spike:** During **§2.2**, confirm **whether Readium sanitizes** loaded documents, document **required CSP**, and add a **security checklist** to the EPUB attach/reader plan (no separate one-off script needed).

**Storage and ops (brief)**

- **Blob storage** for the canonical **`.epub`** matches §2.4; no extra **public** expanded tree.
- **Optional enterprise:** malware scanning of uploads is **out of scope** unless org policy requires it; **encryption.xml** / DRM already covered in §2.1.

---

## 3. Next step (outside this document)

For each **§2** topic, assign an owner, time-box a **spike**, and record **decisions + links** in the architecture roadmap and/or a delivery plan when EPUB is prioritized.
