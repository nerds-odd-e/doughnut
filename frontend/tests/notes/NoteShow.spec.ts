import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import NoteShow from "@/components/notes/NoteShow.vue"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { describe, it, expect, vi, beforeAll, afterEach } from "vitest"

describe("new/updated pink banner", () => {
  beforeAll(() => {
    Date.now = vi.fn(() => new Date(Date.UTC(2017, 1, 14)).valueOf())
  })

  it.each([
    [new Date(Date.UTC(2017, 1, 15)), "rgb(208,237,23)"],
    [new Date(Date.UTC(2017, 1, 13)), "rgb(189,209,64)"],
    [new Date(Date.UTC(2017, 1, 12)), "rgb(181,197,82)"],
    [new Date(Date.UTC(2016, 1, 12)), "rgb(150,150,150)"],
  ])(
    "should show fresher color if recently updated",
    async (updatedAt, expectedColor) => {
      const note = makeMe.aNoteRealm.updatedAtDate(updatedAt).please()
      mockSdkService(NoteController, "showNote", note)

      const wrapper = helper
        .component(NoteShow)
        .withRouter()
        .withCleanStorage()
        .withProps({
          noteId: note.id,
          expandChildren: true,
        })
        .mount({ attachTo: document.body })
      await flushPromises()
      const element = wrapper.find(".note-recent-update-indicator")
        .element as HTMLElement
      const actualColor = element.style.color.replace(/\s/g, "")
      expect(actualColor).toBe(expectedColor.replace(/\s/g, ""))
      wrapper.unmount()
    }
  )
})

describe("note without children", () => {
  const note = makeMe.aNoteRealm.please()

  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>

  const render = (n: typeof note) => {
    mockSdkService(NoteController, "showNote", n)
    wrapper = helper
      .component(NoteShow)
      .withRouter()
      .withCleanStorage()
      .withProps({
        noteId: n.id,
        expandChildren: true,
      })
      .mount({ attachTo: document.body })
  }

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("calls showNote for the note id", () => {
    const showNoteSpy = mockSdkService(NoteController, "showNote", note)
    render(note)

    expect(showNoteSpy).toHaveBeenCalledWith({
      path: { note: note.id },
    })
  })

  it("does not render children control when note has no children", async () => {
    render(note)
    await flushPromises()
    expect(wrapper.findAll('[title="collapse children"]')).toHaveLength(0)
  })
})
