import type { Note } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import type { ComponentPublicInstance } from "vue"
import { TEXT_AUTOSAVE_DEBOUNCE_MS } from "@/composables/useDebouncedTextAutosave"
import {
  editTitle,
  editTitleThenBlur,
  mockedUpdateTitleCall,
  mockUpdateNoteTitle,
  mountNoteTextContent,
  titleEditorEl,
} from "./noteTextContentTestSupport"

describe("NoteTextContent title edit save race", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>

  const mountWith = (note: Note) => {
    wrapper = mountNoteTextContent(note)
    return wrapper
  }

  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers()
    mockUpdateNoteTitle()
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
    vi.useRealTimers()
  })

  it("saves the last title after an earlier in-flight save finishes", async () => {
    const note = makeMe.aNote.title("Dummy Title").please()
    const releaseSaves: Array<() => void> = []
    mockedUpdateTitleCall.mockImplementation((options) => {
      let release!: () => void
      const gate = new Promise<void>((resolve) => {
        release = resolve
      })
      releaseSaves.push(release)
      return gate.then(() =>
        makeMe.aNoteRealm.title(options.body.newTitle).please()
      )
    })

    const titleSave = (newTitle: string) => [
      { path: { note: note.id }, body: { newTitle } },
    ]

    mountWith(note)
    await editTitle(wrapper, "ABC")
    await vi.advanceTimersByTimeAsync(TEXT_AUTOSAVE_DEBOUNCE_MS)
    await flushPromises()

    expect(mockedUpdateTitleCall.mock.calls).toEqual([titleSave("ABC")])

    await editTitleThenBlur(wrapper, "ABCDEF")
    await flushPromises()

    expect(mockedUpdateTitleCall.mock.calls).toEqual([titleSave("ABC")])

    releaseSaves[0]?.()
    await flushPromises()

    expect(mockedUpdateTitleCall.mock.calls).toEqual([
      titleSave("ABC"),
      titleSave("ABCDEF"),
    ])

    releaseSaves[1]?.()
    await flushPromises()
    expect(titleEditorEl(wrapper).innerText).toBe("ABCDEF")
  })
})
