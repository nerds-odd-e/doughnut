import {
  listPropertyValue,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import { propertyRowWithScalar } from "@/utils/noteContentPropertyRows"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const SCALAR_MARKDOWN = `---
example of: "[[A]]"
example of 2: "[[B]]"
---

# Body`

describe("RichMarkdownEditor list-capable preset append", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("appends to exact list-capable keys without folding legacy suffixes", async () => {
    await h.mountEditor(SCALAR_MARKDOWN, { attachToBody: true })

    await h.commitInsertProperty("example of", "[[C]]")
    const last = h.lastEmittedMarkdown()
    const parsed = parseNoteContentMarkdown(last)
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(parsed.properties["example of"]).toEqual(
      listPropertyValue(["[[A]]", "[[C]]"])
    )
    expect(parsed.properties["example of 2"]).toEqual(
      propertyRowWithScalar("example of 2", "[[B]]").value
    )
  })
})
