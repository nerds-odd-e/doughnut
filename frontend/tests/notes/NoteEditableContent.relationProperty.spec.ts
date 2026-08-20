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

  it("shows relation type picker only when noteContent includes relation frontmatter", async () => {
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

    await wrapper.setProps({
      noteContent: `---
topic: training
---

# Body`,
    })
    await flushPromises()
    expect(relationTypeSelectInRow()).toBeNull()

    wrapper.unmount()
  })
})
