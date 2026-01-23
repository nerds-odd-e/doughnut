import NoteShow from "@/components/notes/NoteShow.vue"
import type { NoteRealm } from "@generated/backend"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockShowNoteAccessory, mockSdkService } from "@tests/helpers"
import {
  describe,
  it,
  expect,
  vi,
  beforeAll,
  beforeEach,
  afterEach,
} from "vitest"

describe("new/updated pink banner", () => {
  beforeAll(() => {
    Date.now = vi.fn(() => new Date(Date.UTC(2017, 1, 14)).valueOf())
  })

  beforeEach(() => {
    mockShowNoteAccessory()
  })

  it.each([
    [new Date(Date.UTC(2017, 1, 15)), "rgb(208,237,23)"],
    [new Date(Date.UTC(2017, 1, 13)), "rgb(189,209,64)"],
    [new Date(Date.UTC(2017, 1, 12)), "rgb(181,197,82)"],
    [new Date(Date.UTC(2016, 1, 12)), "rgb(150,150,150)"],
  ])("should show fresher color if recently updated", async (updatedAt, expectedColor) => {
    const note = makeMe.aNoteRealm.updatedAtDate(updatedAt).please()
    mockSdkService("showNote", note)

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
    // Browser might return spaces in rgb values, e.g. "rgb(208, 237, 23)" vs "rgb(208,237,23)"
    // Normalize both expected and actual by removing spaces
    const actualColor = element.style.color.replace(/\s/g, "")
    const expectedColorNormalized = expectedColor.replace(/\s/g, "")
    expect(actualColor).toBe(expectedColorNormalized)
    wrapper.unmount()
  })
})

describe("note wth children", () => {
  const note = makeMe.aNoteRealm.please()

  beforeEach(() => {
    mockShowNoteAccessory()
  })

  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>

  const render = (n: NoteRealm) => {
    mockSdkService("showNote", n)
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

  it("should call the api", () => {
    const showNoteSpy = mockSdkService("showNote", note)
    render(note)

    expect(showNoteSpy).toHaveBeenCalledWith({
      path: { note: note.id },
    })
  })

  it("should not render children control if no child", async () => {
    render(note)
    await flushPromises()
    expect(wrapper.findAll('[title="collapse children"]')).toHaveLength(0)
  })
})
