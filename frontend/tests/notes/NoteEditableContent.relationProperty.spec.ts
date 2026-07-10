import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, afterEach } from "vitest"
import { mountNoteEditableContent } from "./noteEditableContentTestSupport"

describe("NoteEditableContent relation property row in rich mode", () => {
  afterEach(() => {
    document.body.innerHTML = ""
  })

  it.each([
    {
      name: "shows RelationTypeSelectCompact when noteContent include relation frontmatter",
      markdown: `---
relation: parent-of
---

# Body`,
      expectRelationSelect: true,
    },
    {
      name: "omits RelationTypeSelectCompact when noteContent omit relation property",
      markdown: `---
topic: training
---

# Body`,
      expectRelationSelect: false,
    },
  ])("$name", async ({ markdown, expectRelationSelect }) => {
    const wrapper = mountNoteEditableContent(
      { noteId: 99, noteContent: markdown, asMarkdown: false },
      { attachTo: document.body }
    )
    await flushPromises()

    const rfp = wrapper.findComponent({ name: "RichFrontmatterProperties" })
    expect(rfp.exists()).toBe(true)
    expect(
      rfp.findComponent({ name: "RelationTypeSelectCompact" }).exists()
    ).toBe(expectRelationSelect)
    wrapper.unmount()
  })
})
