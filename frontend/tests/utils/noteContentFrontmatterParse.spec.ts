import { describe, expect, it } from "vitest"
import {
  composeNoteContentMarkdown,
  firstScalarValueFromYamlBlock,
  noteImageScalarsFromMarkdown,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"

describe("firstScalarValueFromYamlBlock", () => {
  it("reads first matching key case-insensitively with trim and quotes", () => {
    const yaml = "  Image : '/a/b.png'  \nother: x\n"
    expect(firstScalarValueFromYamlBlock(yaml, "image")).toBe("/a/b.png")
    expect(firstScalarValueFromYamlBlock(yaml, "IMAGE")).toBe("/a/b.png")
  })

  it("returns undefined when key missing or value empty", () => {
    expect(firstScalarValueFromYamlBlock("a: 1\n", "image")).toBeUndefined()
    expect(firstScalarValueFromYamlBlock("image:\n", "image")).toBeUndefined()
  })

  it("skips blank lines and hash comments", () => {
    const yaml = "# c\n\nimage: z\n"
    expect(firstScalarValueFromYamlBlock(yaml, "image")).toBe("z")
  })
})

describe("noteImageScalarsFromMarkdown", () => {
  it("returns image and image_mask from leading frontmatter", () => {
    const md =
      "---\nImage: /attachments/images/1/f.png\nimage_mask: 0 0 1 1\n---\nHi\n"
    expect(noteImageScalarsFromMarkdown(md)).toEqual({
      noteImage: "/attachments/images/1/f.png",
      imageMask: "0 0 1 1",
    })
  })

  it("returns empty object without frontmatter or without image keys", () => {
    expect(noteImageScalarsFromMarkdown("no fence")).toEqual({})
    expect(noteImageScalarsFromMarkdown("---\na: 1\n---\n")).toEqual({})
  })

  it("returns empty object when opening fence has no closing fence", () => {
    expect(noteImageScalarsFromMarkdown("---\nimage: x\n")).toEqual({})
  })
})

describe("parseNoteContentMarkdown", () => {
  it("treats text without opening fence as body-only", () => {
    const md = "hello\n---\nnot-a-fence: true\n"
    const r = parseNoteContentMarkdown(md)
    expect(r).toEqual({ ok: true, properties: {}, body: md })
  })

  it("parses two scalar properties and body after closing fence", () => {
    const md = "---\nalpha: one\nbeta: 2\n---\nParagraph.\n"
    const r = parseNoteContentMarkdown(md)
    expect(r).toEqual({
      ok: true,
      properties: { alpha: "one", beta: "2" },
      body: "Paragraph.\n",
    })
  })

  it("places body immediately after closing fence without extra newline when absent", () => {
    const md = "---\nk: v\n---\nHello"
    const r = parseNoteContentMarkdown(md)
    expect(r.ok && r.body).toBe("Hello")
  })

  it("parses empty frontmatter block as empty properties", () => {
    const md = "---\n---\nContent line\n"
    const r = parseNoteContentMarkdown(md)
    expect(r).toEqual({ ok: true, properties: {}, body: "Content line\n" })
  })

  it("rejects duplicate keys in frontmatter", () => {
    const md = "---\na: 1\na: 2\n---\nbody\n"
    const r = parseNoteContentMarkdown(md)
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.reason).toBe("duplicate_keys")
      expect(r.message.length).toBeGreaterThan(0)
    }
  })

  it("rejects nested mapping values", () => {
    const md = "---\nouter:\n  inner: x\n---\n"
    const r = parseNoteContentMarkdown(md)
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.reason).toBe("unsupported_value")
    }
  })

  it("rejects array values", () => {
    const md = "---\ntags:\n  - a\n---\n"
    const r = parseNoteContentMarkdown(md)
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.reason).toBe("unsupported_value")
    }
  })

  it("rejects malformed YAML", () => {
    const md = "---\nfoo: [\n---\n"
    const r = parseNoteContentMarkdown(md)
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.reason).toBe("malformed_yaml")
    }
  })

  it("rejects opening fence without closing fence", () => {
    const md = "---\na: 1\nstill yaml\n"
    const r = parseNoteContentMarkdown(md)
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.reason).toBe("malformed_frontmatter_fence")
    }
  })

  it("coerces boolean and number scalars to strings", () => {
    const md = "---\nflag: true\nnum: 42\n---\n"
    const r = parseNoteContentMarkdown(md)
    expect(r).toEqual({
      ok: true,
      properties: { flag: "true", num: "42" },
      body: "",
    })
  })

  it("rejects explicit yaml null property value", () => {
    const md = "---\na: null\n---\n"
    const r = parseNoteContentMarkdown(md)
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.reason).toBe("unsupported_value")
    }
  })

  it("rejects root yaml null mapping position", () => {
    const md = "---\nnull\n---\n"
    const r = parseNoteContentMarkdown(md)
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.reason).toBe("unsupported_value")
    }
  })
})

describe("composeNoteContentMarkdown", () => {
  it("returns body only when properties empty", () => {
    expect(composeNoteContentMarkdown({ properties: {}, body: "x\n" })).toBe(
      "x\n"
    )
  })

  it("round-trips simple parse and compose", () => {
    const original =
      "---\ntitle: Hello\nalias: my-note\n---\n## Body\n\nMore.\n"
    const parsed = parseNoteContentMarkdown(original)
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    const composed = composeNoteContentMarkdown({
      properties: parsed.properties,
      body: parsed.body,
    })
    expect(composed).toBe(original)
  })

  it("round-trips empty frontmatter parse with compose adding no fence when props empty", () => {
    const md = "---\n---\nOnly body\n"
    const parsed = parseNoteContentMarkdown(md)
    expect(parsed.ok && parsed.properties).toEqual({})
    if (!parsed.ok) return
    expect(
      composeNoteContentMarkdown({
        properties: parsed.properties,
        body: parsed.body,
      })
    ).toBe("Only body\n")
  })
})
