import {
  listPropertyValue,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import { propertyRowWithScalar } from "@/utils/noteContentPropertyRows"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor list-capable preset append", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("appends another value to exact list-capable key as a list item", async () => {
    const markdown = `---
example of: "[[A]]"
example of 2: "[[B]]"
---

# Body`
    await h.mountEditor(markdown, { attachToBody: true })
    await h.openAddProperty()

    const keyInput = h
      .getWrapper()
      .find('[data-testid="rich-note-property-key"]')
    const valInput = h
      .getWrapper()
      .find('[data-testid="rich-note-property-value"]')
    await keyInput.setValue("example of")
    await h.setWikiPropertyValueField(valInput, "[[C]]")
    await valInput.trigger("blur")

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
    expect(last).toContain('"[[A]]"')
    expect(last).toContain('"[[C]]"')
    expect(last).toContain('example of 2: "[[B]]"')
  })

  it("appends to an existing list value on the exact key", async () => {
    const markdown = `---
example of:
  - "[[A]]"
  - "[[B]]"
---

# Body`
    await h.mountEditor(markdown, { attachToBody: true })
    await h.openAddProperty()

    const keyInput = h
      .getWrapper()
      .find('[data-testid="rich-note-property-key"]')
    const valInput = h
      .getWrapper()
      .find('[data-testid="rich-note-property-value"]')
    await keyInput.setValue("example of")
    await h.setWikiPropertyValueField(valInput, "[[C]]")
    await valInput.trigger("blur")

    const parsed = parseNoteContentMarkdown(h.lastEmittedMarkdown())
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(parsed.properties["example of"]).toEqual(
      listPropertyValue(["[[A]]", "[[B]]", "[[C]]"])
    )
  })

  it("does not fold legacy suffix keys when adding to the exact base key", async () => {
    const markdown = `---
example of: one
example of 2: two
example of 3: three
---

# Body`
    await h.mountEditor(markdown, { attachToBody: true })
    await h.openAddProperty()

    const keyInput = h
      .getWrapper()
      .find('[data-testid="rich-note-property-key"]')
    const valInput = h
      .getWrapper()
      .find('[data-testid="rich-note-property-value"]')
    await keyInput.setValue("example of")
    await h.setWikiPropertyValueField(valInput, "four")
    await valInput.trigger("blur")

    const parsed = parseNoteContentMarkdown(h.lastEmittedMarkdown())
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
