import { describe, expect, it } from "vitest"
import {
  composeNoteDetailsMarkdown,
  parseNoteDetailsMarkdown,
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
