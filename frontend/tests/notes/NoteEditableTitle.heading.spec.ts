import makeMe from "donut-test-fixtures/makeMe"
import { mountNoteEditableTitle } from "@tests/notes/spokenTitleTestSupport"
import type { VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import { afterEach, describe, expect, it } from "vitest"

const bodySectionHeadingFontSizePx = 24

describe("NoteEditableTitle heading", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>

  afterEach(() => {
    wrapper.unmount()
  })

  it.each([
    { mode: "editable", readonly: false },
    { mode: "readonly", readonly: true },
  ])(
    "shows the $mode title bold and larger than a body section heading",
    ({ readonly }) => {
      const note = makeMe.aNote.title("Orchard notes").please()
      wrapper = mountNoteEditableTitle({
        noteTopology: note.noteTopology,
        noteId: note.id,
        readonly,
      })

      const style = getComputedStyle(wrapper.find("h2").element)

      expect(Number(style.fontWeight)).toBeGreaterThanOrEqual(700)
      expect(Number.parseFloat(style.fontSize)).toBeGreaterThan(
        bodySectionHeadingFontSizePx
      )
    }
  )
})
