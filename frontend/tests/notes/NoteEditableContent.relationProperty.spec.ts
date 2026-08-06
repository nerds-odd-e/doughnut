import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, afterEach } from "vitest"
import { mountNoteEditableContent } from "./noteEditableContentTestSupport"

function relationTypeSelectInRow(): Element | null {
  return document.querySelector(
    '[data-testid="rich-note-property-row"][data-property-key="relation"] button[aria-label="Relation Type"]'
  )
}

describe("NoteEditableContent relation property row in rich mode", () => {
  afterEach(() => {
    document.body.innerHTML = ""
  })

  it("shows relation type picker when noteContent includes relation frontmatter", async () => {
    const wrapper = mountNoteEditableContent(
      {
        noteId: 99,
        noteContent: `---
relation: parent-of
---

# Body`,
        asMarkdown: false,
      },
      { attachTo: document.body }
    )
    await flushPromises()

    expect(relationTypeSelectInRow()).not.toBeNull()
    wrapper.unmount()
  })

  it("omits relation type picker when noteContent has no relation property", async () => {
    const wrapper = mountNoteEditableContent(
      {
        noteId: 99,
        noteContent: `---
topic: training
---

# Body`,
        asMarkdown: false,
      },
      { attachTo: document.body }
    )
    await flushPromises()

    expect(relationTypeSelectInRow()).toBeNull()
    wrapper.unmount()
  })
})
