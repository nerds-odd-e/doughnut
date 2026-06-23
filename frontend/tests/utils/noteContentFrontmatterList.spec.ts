import { describe, expect, it } from "vitest"
import {
  composeNoteContentMarkdown,
  isListPropertyValue,
  listPropertyValue,
  notePropertiesFromScalarRecord,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"

describe("parseNoteContentMarkdown list values", () => {
  it("parses block-sequence list values with string items", () => {
    const md = "---\ntags:\n  - alpha\n  - beta\n---\nbody\n"
    const r = parseNoteContentMarkdown(md)
    expect(r.ok).toBe(true)
    if (!r.ok) return
    expect(r.properties.tags).toEqual(listPropertyValue(["alpha", "beta"]))
    expect(r.body).toBe("body\n")
  })

  it("parses flow-array list values", () => {
    const md = "---\nexample of: [one, two]\n---\n"
    const r = parseNoteContentMarkdown(md)
    expect(r.ok).toBe(true)
    if (!r.ok) return
    expect(r.properties["example of"]).toEqual(
      listPropertyValue(["one", "two"])
    )
  })

  it("normalizes list item numbers and booleans to strings", () => {
    const md = "---\nnums: [1, 2]\nflags:\n  - true\n  - false\n---\n"
    const r = parseNoteContentMarkdown(md)
    expect(r.ok).toBe(true)
    if (!r.ok) return
    expect(r.properties.nums).toEqual(listPropertyValue(["1", "2"]))
    expect(r.properties.flags).toEqual(listPropertyValue(["true", "false"]))
  })

  it("parses empty lists", () => {
    const md = "---\ntags: []\n---\n"
    const r = parseNoteContentMarkdown(md)
    expect(r.ok).toBe(true)
    if (!r.ok) return
    expect(r.properties.tags).toEqual(listPropertyValue([]))
  })

  it("allows duplicate list items", () => {
    const md = "---\ntags:\n  - dup\n  - dup\n---\n"
    const r = parseNoteContentMarkdown(md)
    expect(r.ok).toBe(true)
    if (!r.ok) return
    expect(r.properties.tags).toEqual(listPropertyValue(["dup", "dup"]))
  })

  it("parses Obsidian passthrough keys tags aliases cssclasses as lists", () => {
    const md =
      "---\ntags:\n  - t1\naliases: [a1, a2]\ncssclasses:\n  - c1\n---\n"
    const r = parseNoteContentMarkdown(md)
    expect(r.ok).toBe(true)
    if (!r.ok) return
    expect(r.properties.tags).toEqual(listPropertyValue(["t1"]))
    expect(r.properties.aliases).toEqual(listPropertyValue(["a1", "a2"]))
    expect(r.properties.cssclasses).toEqual(listPropertyValue(["c1"]))
  })

  it("rejects null list items", () => {
    const md = "---\ntags:\n  - a\n  - null\n---\n"
    const r = parseNoteContentMarkdown(md)
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.reason).toBe("unsupported_value")
    }
  })

  it("rejects nested lists in list items", () => {
    const md = "---\ntags:\n  - [nested]\n---\n"
    const r = parseNoteContentMarkdown(md)
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.reason).toBe("unsupported_value")
    }
  })

  it("rejects list values on scalar-only structural keys", () => {
    for (const key of [
      "image",
      "image_mask",
      "wikidata_id",
      "title_pattern",
      "question_generation_instruction",
      "type",
      "relation",
      "source",
      "target",
    ]) {
      const md = `---\n${key}:\n  - x\n---\n`
      const r = parseNoteContentMarkdown(md)
      expect(r.ok, key).toBe(false)
      if (!r.ok) {
        expect(r.reason, key).toBe("unsupported_value")
      }
    }
  })
})

describe("composeNoteContentMarkdown list values", () => {
  it("composes non-empty lists in Obsidian block style", () => {
    const composed = composeNoteContentMarkdown({
      properties: notePropertiesFromScalarRecord({
        title: "Hello",
      }),
      body: "Body\n",
    })
    const withList = composeNoteContentMarkdown({
      properties: {
        ...notePropertiesFromScalarRecord({ title: "Hello" }),
        tags: listPropertyValue(["t1", "t2"]),
      },
      body: "Body\n",
    })
    expect(withList).toBe(
      "---\ntitle: Hello\ntags:\n  - t1\n  - t2\n---\nBody\n"
    )
    expect(composed).toBe("---\ntitle: Hello\n---\nBody\n")
  })

  it("composes empty lists as key: []", () => {
    const composed = composeNoteContentMarkdown({
      properties: {
        tags: listPropertyValue([]),
      },
      body: "x\n",
    })
    expect(composed).toBe("---\ntags: []\n---\nx\n")
  })

  it("round-trips list frontmatter through parse and compose", () => {
    const original =
      "---\ntags:\n  - alpha\n  - beta\naliases: []\nstatus: active\n---\n## Body\n"
    const parsed = parseNoteContentMarkdown(original)
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(isListPropertyValue(parsed.properties.tags!)).toBe(true)
    const composed = composeNoteContentMarkdown({
      properties: parsed.properties,
      body: parsed.body,
    })
    expect(composed).toBe(original)
  })

  it("round-trips flow-array parse into block-style compose", () => {
    const md = "---\nexample of: [one, two]\n---\nBody\n"
    const parsed = parseNoteContentMarkdown(md)
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    const composed = composeNoteContentMarkdown({
      properties: parsed.properties,
      body: parsed.body,
    })
    expect(composed).toBe("---\nexample of:\n  - one\n  - two\n---\nBody\n")
  })

  it("preserves duplicate list items on round-trip", () => {
    const original = "---\ntags:\n  - dup\n  - dup\n---\n"
    const parsed = parseNoteContentMarkdown(original)
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(
      composeNoteContentMarkdown({
        properties: parsed.properties,
        body: parsed.body,
      })
    ).toBe(original)
  })
})
