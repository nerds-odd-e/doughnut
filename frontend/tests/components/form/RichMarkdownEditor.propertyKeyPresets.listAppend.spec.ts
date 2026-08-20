import { flushPromises } from "@vue/test-utils"
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

const LIST_MARKDOWN = `---
example of:
  - "[[A]]"
  - "[[B]]"
---

# Body`

const LEGACY_SUFFIX_MARKDOWN = `---
example of: one
example of 2: two
example of 3: three
---

# Body`

describe("RichMarkdownEditor list-capable preset append", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("appends to exact list-capable keys without folding legacy suffixes", async () => {
    const wrapper = await h.mountEditor(SCALAR_MARKDOWN, { attachToBody: true })

    await h.commitInsertProperty("example of", "[[C]]")
    const last = h.lastEmittedMarkdown()
    let parsed = parseNoteContentMarkdown(last)
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(parsed.properties["example of"]).toEqual(
      listPropertyValue(["[[A]]", "[[C]]"])
    )
    expect(parsed.properties["example of 2"]).toEqual(
      propertyRowWithScalar("example of 2", "[[B]]").value
    )
    expect(last).toContain('"[[A]]"')
    expect(last).toContain('"[[C]]"')
    expect(last).toContain('example of 2: "[[B]]"')

    await wrapper.setProps({ modelValue: LIST_MARKDOWN })
    await flushPromises()
    await h.commitInsertProperty("example of", "[[C]]")
    parsed = parseNoteContentMarkdown(h.lastEmittedMarkdown())
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(parsed.properties["example of"]).toEqual(
      listPropertyValue(["[[A]]", "[[B]]", "[[C]]"])
    )

    await wrapper.setProps({ modelValue: LEGACY_SUFFIX_MARKDOWN })
    await flushPromises()
    await h.commitInsertProperty("example of", "four")
    parsed = parseNoteContentMarkdown(h.lastEmittedMarkdown())
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(parsed.properties["example of"]).toEqual(
      listPropertyValue(["one", "four"])
    )
    expect(parsed.properties["example of 2"]).toEqual(
      propertyRowWithScalar("example of 2", "two").value
    )
    expect(parsed.properties["example of 3"]).toEqual(
      propertyRowWithScalar("example of 3", "three").value
    )
  })
})
