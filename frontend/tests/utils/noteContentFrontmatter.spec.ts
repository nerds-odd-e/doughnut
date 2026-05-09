import { describe, expect, it } from "vitest"
import {
  composeNoteContentFromPropertyRows,
  composeNoteContentMarkdown,
  contentHasRelationProperty,
  firstScalarValueFromYamlBlock,
  insertPropertyRowAt,
  isImagePropertyKey,
  isUrlPropertyKey,
  isWikidataIdPropertyKey,
  noteImageScalarsFromMarkdown,
  parseNoteContentMarkdown,
  propertyRecordHasRelationKey,
  removePropertyRowAt,
  renamePropertyRowKeyAt,
  richModeKeyDropdownPresetKeys,
  richModeKeyDropdownPresetKeysForPropertyRows,
  sortedPropertyRowsFromRecord,
  titlePatternFromNoteMarkdown,
  validatePropertyRowsForRichEdit,
} from "@/utils/noteContentFrontmatter"

describe("richModeKeyDropdownPresetKeysForPropertyRows", () => {
  it("matches full list when no rows have keys that occupy preset slots", () => {
    expect(richModeKeyDropdownPresetKeysForPropertyRows(false, [])).toEqual(
      richModeKeyDropdownPresetKeys(false)
    )
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        { key: "status", value: "ok" },
      ])
    ).toEqual(richModeKeyDropdownPresetKeys(false))
  })

  it("removes image when a row uses the image key", () => {
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        { key: "image", value: "/a.png" },
      ])
    ).toEqual(["wikidata_id", "url"])
  })

  it("removes wikidata_id for wikidataId alias", () => {
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        { key: "wikidataId", value: "Q1" },
      ])
    ).toEqual(["image", "url"])
  })

  it("removes url when url is present", () => {
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        { key: "url", value: "https://x" },
      ])
    ).toEqual(["image", "wikidata_id"])
  })

  it("ignores rows with empty keys", () => {
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        { key: "", value: "x" },
        { key: "  ", value: "y" },
      ])
    ).toEqual(richModeKeyDropdownPresetKeys(false))
  })

  it("in index context, removes index-only presets when placeholder rows exist", () => {
    const rows = [
      { key: "title_pattern", value: "" },
      { key: "question_generation_instruction", value: "" },
    ]
    expect(richModeKeyDropdownPresetKeysForPropertyRows(true, rows)).toEqual([
      "image",
      "wikidata_id",
      "url",
    ])
  })
})

describe("richModeKeyDropdownPresetKeys", () => {
  it("appends index-only keys when isIndexContext is true", () => {
    expect(richModeKeyDropdownPresetKeys(false)).toEqual([
      "image",
      "wikidata_id",
      "url",
    ])
    expect(richModeKeyDropdownPresetKeys(true)).toEqual([
      "image",
      "wikidata_id",
      "url",
      "title_pattern",
      "question_generation_instruction",
    ])
  })
})

describe("isImagePropertyKey", () => {
  it("matches image case-insensitively with trim", () => {
    expect(isImagePropertyKey("image")).toBe(true)
    expect(isImagePropertyKey(" Image ")).toBe(true)
    expect(isImagePropertyKey("IMAGE")).toBe(true)
    expect(isImagePropertyKey("image_mask")).toBe(false)
    expect(isImagePropertyKey("imagery")).toBe(false)
  })
})

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

describe("property rows compose / mutate", () => {
  it("insert: composing first row yields parseable frontmatter with matching scalar props", () => {
    const rows = insertPropertyRowAt([], 0, { key: "topic", value: "training" })
    const md = composeNoteContentFromPropertyRows(rows, "# Hello\n")
    const parsed = parseNoteContentMarkdown(md)
    expect(parsed.ok && parsed.properties).toEqual({
      topic: "training",
    })
    if (parsed.ok) {
      expect(parsed.body).toContain("# Hello")
    }
  })

  it("rename: composed markdown loses old key and exposes new key", () => {
    const parsed = parseNoteContentMarkdown(
      "---\nalpha: one\nbeta: two\n---\nBody\n"
    )
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    let rows = sortedPropertyRowsFromRecord(parsed.properties)
    rows = renamePropertyRowKeyAt(rows, 0, "alphaRenamed")
    const md = composeNoteContentFromPropertyRows(rows, parsed.body)
    const again = parseNoteContentMarkdown(md)
    expect(again.ok && again.properties).toEqual({
      alphaRenamed: "one",
      beta: "two",
    })
    expect(again.ok && again.properties.alpha).toBeUndefined()
  })

  it("remove: dropping one row leaves remaining props only", () => {
    const parsed = parseNoteContentMarkdown("---\na: 1\nb: 2\n---\nRest\n")
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    let rows = sortedPropertyRowsFromRecord(parsed.properties)
    rows = removePropertyRowAt(rows, 0)
    const md = composeNoteContentFromPropertyRows(rows, parsed.body)
    const again = parseNoteContentMarkdown(md)
    expect(again.ok && again.properties).toEqual({ b: "2" })
  })

  it("remove: dropping second sorted row keeps first key only", () => {
    const parsed = parseNoteContentMarkdown("---\na: 1\nb: 2\n---\nRest\n")
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    let rows = sortedPropertyRowsFromRecord(parsed.properties)
    rows = removePropertyRowAt(rows, 1)
    const md = composeNoteContentFromPropertyRows(rows, parsed.body)
    const again = parseNoteContentMarkdown(md)
    expect(again.ok && again.properties).toEqual({ a: "1" })
    expect(again.ok && again.properties.b).toBeUndefined()
  })

  it("remove: clearing every row yields body-only content without frontmatter fence", () => {
    const parsed = parseNoteContentMarkdown("---\na: 1\nb: 2\n---\nRest\n")
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    let rows = sortedPropertyRowsFromRecord(parsed.properties)
    rows = removePropertyRowAt(rows, 0)
    rows = removePropertyRowAt(rows, 0)
    const md = composeNoteContentFromPropertyRows(rows, parsed.body)
    expect(md).toBe("Rest\n")
    expect(md.startsWith("---")).toBe(false)
    const again = parseNoteContentMarkdown(md)
    expect(again.ok && again.properties).toEqual({})
    if (again.ok) expect(again.body).toBe("Rest\n")
  })

  it("remove: dropping sole row yields omitted frontmatter and unchanged body", () => {
    const parsed = parseNoteContentMarkdown("---\nonly: x\n---\nParagraph.\n")
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    let rows = sortedPropertyRowsFromRecord(parsed.properties)
    rows = removePropertyRowAt(rows, 0)
    const md = composeNoteContentFromPropertyRows(rows, parsed.body)
    expect(md).toBe("Paragraph.\n")
    expect(md.startsWith("---")).toBe(false)
    const again = parseNoteContentMarkdown(md)
    expect(again.ok && again.properties).toEqual({})
    if (again.ok) expect(again.body).toBe("Paragraph.\n")
  })
})

describe("contentHasRelationProperty", () => {
  it("returns true when relation key exists (case-insensitive)", () => {
    expect(
      contentHasRelationProperty("---\nrelation: parent-of\n---\n\nbody\n")
    ).toBe(true)
    expect(contentHasRelationProperty("---\nRelation: child-of\n---\n\n")).toBe(
      true
    )
  })

  it("returns false without relation key", () => {
    expect(contentHasRelationProperty("---\ntopic: x\n---\n")).toBe(false)
    expect(contentHasRelationProperty("no frontmatter")).toBe(false)
  })

  it("returns false on parse failure", () => {
    expect(contentHasRelationProperty("---\nbad:\n  nested: x\n---\n")).toBe(
      false
    )
  })
})

describe("propertyRecordHasRelationKey", () => {
  it("detects relation with surrounding whitespace on key", () => {
    expect(propertyRecordHasRelationKey({ " relation ": "x" })).toBe(true)
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

  it("allows one draft row with empty key when value is non-empty", () => {
    expect(
      validatePropertyRowsForRichEdit([{ key: "   ", value: "[[Note]]" }])
    ).toEqual({ ok: true })
  })

  it("rejects empty key when value is empty", () => {
    const r = validatePropertyRowsForRichEdit([{ key: "", value: "  " }])
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.message).toContain("empty key")
    }
  })

  it("rejects more than one row with empty key", () => {
    const r = validatePropertyRowsForRichEdit([
      { key: "", value: "a" },
      { key: "  ", value: "b" },
    ])
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.message).toContain("Only one property")
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

describe("isWikidataIdPropertyKey", () => {
  it("matches wikidata_id with varied casing and spacing", () => {
    expect(isWikidataIdPropertyKey("wikidata_id")).toBe(true)
    expect(isWikidataIdPropertyKey("  WikiData_ID ")).toBe(true)
  })

  it("matches camelCase wikidataId from YAML", () => {
    expect(isWikidataIdPropertyKey("wikidataId")).toBe(true)
  })

  it("does not match unrelated keys", () => {
    expect(isWikidataIdPropertyKey("relation")).toBe(false)
    expect(isWikidataIdPropertyKey("wikidata")).toBe(false)
  })
})

describe("isUrlPropertyKey", () => {
  it("matches url with varied casing and spacing", () => {
    expect(isUrlPropertyKey("url")).toBe(true)
    expect(isUrlPropertyKey("  URL ")).toBe(true)
  })

  it("does not match other keys", () => {
    expect(isUrlPropertyKey("urls")).toBe(false)
    expect(isUrlPropertyKey("wikidata_id")).toBe(false)
  })
})

describe("titlePatternFromNoteMarkdown", () => {
  it("reads title_pattern and legacy TitlePattern keys", () => {
    expect(
      titlePatternFromNoteMarkdown('---\ntitle_pattern: "{{date}}"\n---\n')
    ).toBe("{{date}}")
    const md = '---\nTitlePattern: "{{date}}"\n---\n'
    expect(titlePatternFromNoteMarkdown(md)).toBe("{{date}}")
  })

  it("returns undefined when key missing or blank", () => {
    expect(titlePatternFromNoteMarkdown("---\na: 1\n---\n")).toBeUndefined()
    expect(
      titlePatternFromNoteMarkdown('---\ntitle_pattern: ""\n---\n')
    ).toBeUndefined()
  })
})
