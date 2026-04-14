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

### 2.3 Locators: “page + bbox” for EPUB (your topic)

- PDF **`PageBbox`** is **page index + normalized bbox** in **MinerU 0–1000** space. What is the **canonical EPUB locator** for the **same conceptual jobs**: block start, direct-content regions, **reading position**, future **citation** (`SourceSpan`)?
- Candidates to evaluate: **EPUB CFI**, **XPath + fragment**, **spine index + file offset**, **character offset in flattened text**, **CSS selector + range** — which are **stable** under **reflow**, **font change**, and **user stylesheet**?
- Do we **virtualize “pages”** for EPUB to reuse **`pageIndex` + `normalizedY`**, or **replace** reading-position schema for non-PDF formats?
- How do we implement **viewport → current block** when there is **no pixel bbox** (pure reflow HTML)?

### 2.4 Data model and API

- Should **`BookContentBlock`** rows **reuse** the same persistence shape for EPUB (type, order, optional **pageIndex**/bbox analog, raw payload), or **introduce** a parallel/import-specific table?
- Should **`allBboxes`** remain the **wire format** for EPUB, or should **`PageBbox`** be generalized (e.g. **union type**: PDF bbox vs EPUB CFI range)?
- **`GET …/book/file`:** serve **raw `.epub`** only, or also **expanded** artifacts (security + caching implications)?

### 2.5 CLI and attach UX

- **`/attach`**: same command with **format sniffing**, or **`/attach-epub`**? How does the CLI **run extraction** (local Node script, call backend pre-process, or upload-only)?

### 2.6 Server vs client work split

- Is **unpack + parse + normalize** done **server-side** (consistent with MinerU for PDF) or **client-side** (privacy, cost)? Implications for **tests**, **repeatability**, and **large files**.

### 2.7 Parity gaps that are PDF-specific today

- **Pinch/zoom vs reflow:** what is EPUB’s equivalent of **pdf.js `currentScale`** for **reading position** and **control panel geometry**?
- **“Content block” hit targets:** PDF uses **overlay / MinerU geometry** for long-press; what is the EPUB **equivalent** (DOM range, paragraph id, CFI span)?
- **E2E:** what replaces **canvas OCR** for **deterministic** assertions?

### 2.8 Product edge cases

- **Footnotes, endnotes, sidebars:** are they **in stream** or **hyperlinked** — how does that affect **direct content** and **reading order**?
- **Tables, code blocks, images:** mapping to **`BookContentBlock` types** and **hasDirectContent**.
- **Multiple renditions** in one EPUB (if present): which one is **canonical**?

### 2.9 Legal, storage, and ops

- **Licensing** of third-party EPUB renderers; **CSP** if **iframe** or **inline HTML**; **sanitization** of XHTML to prevent XSS when rendering user-provided EPUBs.

---

## 3. Next step (outside this document)

For each **§2** topic, assign an owner, time-box a **spike**, and record **decisions + links** in the architecture roadmap and/or a delivery plan when EPUB is prioritized.
