---
id: SEED-125
status: active
planted: 2026-11-12
scope: story
---

# Read notes through MCP by Portable path

<a id="read-notes-through-mcp-by-portable-path"></a>

### Read notes through MCP by Portable path

**Identity:** SEED-125#read-notes-through-mcp-by-portable-path
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal.** A Donut user can ask their connected assistant to follow a wiki link
and get the intended note or its graph context using the same address they use
in their notes.

**Scope.** Use the existing wiki-link resolution rules for `read_note` and
`get_note_graph`. A call with a title or alias needs an explicit notebook scope
because MCP has no source note. A title takes precedence over an alias. If
several readable notes match, give the caller enough notebook and folder
information to choose, including when notebooks with the same name belong to
different owners. `list_note_candidates` supplies qualified Portable paths that
the read and graph tools accept directly. MCP-generated note references use the
same address, and full reads supply it with the notebook's `acceptedRevision`
when available. Return authored Markdown verbatim.

Delete numeric note ID arguments and MCP-generated note ID fields, together with
their dedicated instructions and fixtures. Sweep the tool schemas, responses,
documentation, and callers for remnants of the removed contract.

**Key examples.**

- A user gives their assistant `[[Engineering:Practices/Code Review]]`. The
  assistant can read that note or request its graph with the same destination.
  The full read supplies the qualified path and accepted revision for citation.
- In `Engineering`, two readable notes have the title `Code Review`. A title
  lookup gives the assistant both choices, and the path for the selected
  candidate leads to the intended note.

Portable paths describe a note's location and change when it moves or is
renamed. See [ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#links-and-attachments)
for path syntax and resolution, and [ADR 0005 — Web routes](../../docs/adrs/0005-web-routes-accepted.md#notebook-links-as-web-destinations)
for links that appear inside authored Markdown.
