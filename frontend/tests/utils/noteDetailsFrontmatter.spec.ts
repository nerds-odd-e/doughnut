import { describe, expect, it } from "vitest"
import {
  composeNoteDetailsFromPropertyRows,
  composeNoteDetailsMarkdown,
  insertPropertyRowAt,
  parseNoteDetailsMarkdown,
  removePropertyRowAt,
  renamePropertyRowKeyAt,
  sortedPropertyRowsFromRecord,
  validatePropertyRowsForRichEdit,
} from "@/utils/noteDetailsFrontmatter"

describe("parseNoteDetailsMarkdown", () => {
  it("treats text without opening fence as body-only", () => {
    const md = "hello\n---\nnot-a-fence: true\n"
    const r = parseNoteDetailsMarkdown(md)
    expect(r).toEqual({ ok: true, properties: {}, body: md })
  })

  it("parses two scalar properties and body after closing fence", () => {
    const md = "---\nalpha: one\nbeta: 2\n---\nParagraph.\n"
    const r = parseNoteDetailsMarkdown(md)
    expect(r).toEqual({
      ok: true,
      properties: { alpha: "one", beta: "2" },
      body: "Paragraph.\n",
    })
  })

  it("places body immediately after closing fence without extra newline when absent", () => {
    const md = "---\nk: v\n---\nHello"
    const r = parseNoteDetailsMarkdown(md)
    expect(r.ok && r.body).toBe("Hello")
  })

  it("parses empty frontmatter block as empty properties", () => {
    const md = "---\n---\nContent line\n"
    const r = parseNoteDetailsMarkdown(md)
    expect(r).toEqual({ ok: true, properties: {}, body: "Content line\n" })
  })

  it("rejects duplicate keys in frontmatter", () => {
    const md = "---\na: 1\na: 2\n---\nbody\n"
    const r = parseNoteDetailsMarkdown(md)
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.reason).toBe("duplicate_keys")
      expect(r.message.length).toBeGreaterThan(0)
    }
  })

  it("rejects nested mapping values", () => {
    const md = "---\nouter:\n  inner: x\n---\n"
    const r = parseNoteDetailsMarkdown(md)
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.reason).toBe("unsupported_value")
    }
  })

  it("rejects array values", () => {
    const md = "---\ntags:\n  - a\n---\n"
    const r = parseNoteDetailsMarkdown(md)
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.reason).toBe("unsupported_value")
    }
  })

  it("rejects malformed YAML", () => {
    const md = "---\nfoo: [\n---\n"
    const r = parseNoteDetailsMarkdown(md)
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.reason).toBe("malformed_yaml")
    }
  })

  it("rejects opening fence without closing fence", () => {
    const md = "---\na: 1\nstill yaml\n"
    const r = parseNoteDetailsMarkdown(md)
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.reason).toBe("malformed_frontmatter_fence")
    }
  })

  it("coerces boolean and number scalars to strings", () => {
    const md = "---\nflag: true\nnum: 42\n---\n"
    const r = parseNoteDetailsMarkdown(md)
    expect(r).toEqual({
      ok: true,
      properties: { flag: "true", num: "42" },
      body: "",
    })
  })

  it("rejects explicit yaml null property value", () => {
    const md = "---\na: null\n---\n"
    const r = parseNoteDetailsMarkdown(md)
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.reason).toBe("unsupported_value")
    }
  })

  it("rejects root yaml null mapping position", () => {
    const md = "---\nnull\n---\n"
    const r = parseNoteDetailsMarkdown(md)
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.reason).toBe("unsupported_value")
    }
  })
})

describe("composeNoteDetailsMarkdown", () => {
  it("returns body only when properties empty", () => {
    expect(composeNoteDetailsMarkdown({ properties: {}, body: "x\n" })).toBe(
      "x\n"
    )
  })

  it("round-trips simple parse and compose", () => {
    const original = "---\ntitle: Hello\nslug: my-note\n---\n## Body\n\nMore.\n"
    const parsed = parseNoteDetailsMarkdown(original)
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    const composed = composeNoteDetailsMarkdown({
      properties: parsed.properties,
      body: parsed.body,
    })
    expect(composed).toBe(original)
  })

  it("round-trips empty frontmatter parse with compose adding no fence when props empty", () => {
    const md = "---\n---\nOnly body\n"
    const parsed = parseNoteDetailsMarkdown(md)
    expect(parsed.ok && parsed.properties).toEqual({})
    if (!parsed.ok) return
    expect(
      composeNoteDetailsMarkdown({
        properties: parsed.properties,
        body: parsed.body,
      })
    ).toBe("Only body\n")
  })
})

describe("property rows compose / mutate", () => {
  it("insert: composing first row yields parseable frontmatter with matching scalar props", () => {
    const rows = insertPropertyRowAt([], 0, { key: "topic", value: "training" })
    const md = composeNoteDetailsFromPropertyRows(rows, "# Hello\n")
    const parsed = parseNoteDetailsMarkdown(md)
    expect(parsed.ok && parsed.properties).toEqual({
      topic: "training",
    })
    if (parsed.ok) {
      expect(parsed.body).toContain("# Hello")
    }
  })

  it("rename: composed markdown loses old key and exposes new key", () => {
    const parsed = parseNoteDetailsMarkdown(
      "---\nalpha: one\nbeta: two\n---\nBody\n"
    )
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    let rows = sortedPropertyRowsFromRecord(parsed.properties)
    rows = renamePropertyRowKeyAt(rows, 0, "alphaRenamed")
    const md = composeNoteDetailsFromPropertyRows(rows, parsed.body)
    const again = parseNoteDetailsMarkdown(md)
    expect(again.ok && again.properties).toEqual({
      alphaRenamed: "one",
      beta: "two",
    })
    expect(again.ok && again.properties.alpha).toBeUndefined()
  })

  it("remove: dropping one row leaves remaining props only", () => {
    const parsed = parseNoteDetailsMarkdown("---\na: 1\nb: 2\n---\nRest\n")
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    let rows = sortedPropertyRowsFromRecord(parsed.properties)
    rows = removePropertyRowAt(rows, 0)
    const md = composeNoteDetailsFromPropertyRows(rows, parsed.body)
    const again = parseNoteDetailsMarkdown(md)
    expect(again.ok && again.properties).toEqual({ b: "2" })
  })
})

describe("validatePropertyRowsForRichEdit", () => {
  it("accepts distinct keys after trim", () => {
    expect(
      validatePropertyRowsForRichEdit([
        { key: " a ", value: "x" },
        { key: "b", value: " y " },
      ])
    ).toEqual({ ok: true })
  })

  it("rejects empty keys", () => {
    const r = validatePropertyRowsForRichEdit([{ key: "   ", value: "x" }])
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.message).toContain("empty")
    }
  })

  it("rejects duplicate keys after trim", () => {
    const r = validatePropertyRowsForRichEdit([
      { key: "same", value: "a" },
      { key: "same", value: "b" },
    ])
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.message).toContain("Duplicate")
    }
  })
})
