import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import {
  MemoryTrackerController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { advanceNoteContentSaveDebounce } from "@tests/helpers/noteContentDebounceTestSupport"
import {
  mountNoteEditableContent,
  setTextareaValue,
  setupPopupsMock,
  setupUpdateNoteContentMock,
} from "./noteEditableContentTestSupport"
import {
  clickListAdd,
  clickModeTab,
  openPropertyValueDialog,
  savePropertyValueDialog,
  setListItemValue,
} from "../components/form/propertyValueDialogTestDom"

vi.mock("@/components/commons/Popups/usePopups")

describe("NoteEditableContent: a tracked single value that becomes a list", () => {
  const noteId = 1
  const singleValue = `---\nexample of: "[[run]]"\n---\n\nBody.`
  let followSpy: ReturnType<typeof mockSdkService>
  let updateNoteContentSpy: ReturnType<typeof setupUpdateNoteContentMock>
  let confirmMock: ReturnType<typeof vi.fn<(msg: string) => Promise<boolean>>>

  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers()
    updateNoteContentSpy = setupUpdateNoteContentMock()
    confirmMock = vi.fn<(msg: string) => Promise<boolean>>()
    setupPopupsMock(vi.fn().mockResolvedValue(null), { confirm: confirmMock })
    mockSdkService(
      NoteController,
      "getNoteInfo",
      makeMe.aNoteRecallInfo
        .memoryTrackers([
          makeMe.aMemoryTracker.id(7).withPropertyKey("example of").please(),
        ])
        .please()
    )
    followSpy = mockSdkService(
      MemoryTrackerController,
      "followPropertyValue",
      undefined
    )
  })

  afterEach(() => {
    document.body.innerHTML = ""
    vi.restoreAllMocks()
    vi.useRealTimers()
  })

  async function saveMarkdown(edited: string) {
    const wrapper = await mountNoteEditableContent({
      noteId,
      noteContent: singleValue,
    })
    await setTextareaValue(wrapper, edited)
    await advanceNoteContentSaveDebounce()
    expect(updateNoteContentSpy).toHaveBeenCalled()
    wrapper.unmount()
  }

  const followedTo = (propertyValue: string) => ({
    path: { note: noteId },
    body: { propertyKey: "example of", propertyValue },
  })

  it("moves the tracker onto the former value, silently, when the Markdown editor adds a value", async () => {
    await saveMarkdown(
      `---\nexample of:\n  - "[[run]]"\n  - "[[past tense]]"\n---\n\nBody.`
    )

    expect(followSpy).toHaveBeenCalledOnce()
    expect(followSpy).toHaveBeenCalledWith(followedTo("[[run]]"))
    expect(confirmMock).not.toHaveBeenCalled()
  })

  it("moves nothing when the former value is not in the new list", async () => {
    await saveMarkdown(
      `---\nexample of:\n  - "[[walk]]"\n  - "[[past tense]]"\n---\n\nBody.`
    )

    expect(followSpy).not.toHaveBeenCalled()
  })

  describe("in the rich editor", () => {
    async function saveRich(
      edit: (wrapper: VueWrapper<ComponentPublicInstance>) => Promise<void>
    ) {
      const wrapper = (await mountNoteEditableContent(
        { noteId, noteContent: singleValue, asMarkdown: false },
        { attachTo: document.body }
      )) as VueWrapper<ComponentPublicInstance>
      await edit(wrapper)
      await advanceNoteContentSaveDebounce()
      expect(updateNoteContentSpy).toHaveBeenCalled()
      wrapper.unmount()
    }

    it("moves the tracker onto the former value when a row appends a value", async () => {
      await saveRich((wrapper) =>
        appendInRichRow(wrapper, "example of", "[[past tense]]")
      )

      expect(followSpy).toHaveBeenCalledWith(followedTo("[[run]]"))
    })

    it("moves the tracker onto the former value when the list dialog adds a value", async () => {
      await saveRich(async (wrapper) => {
        await openPropertyValueDialog(wrapper)
        clickModeTab("list")
        await flushPromises()
        clickListAdd()
        await flushPromises()
        setListItemValue(1, "[[past tense]]")
        await flushPromises()
        await savePropertyValueDialog()
      })

      expect(followSpy).toHaveBeenCalledWith(followedTo("[[run]]"))
    })
  })
})

async function appendInRichRow(
  wrapper: VueWrapper<ComponentPublicInstance>,
  key: string,
  value: string
) {
  const addButton = wrapper
    .findAll("button")
    .find((button) => button.text().includes("Add property"))
  ;(addButton!.element as HTMLButtonElement).click()
  await flushPromises()
  await wrapper.find('[data-testid="rich-note-property-key"]').setValue(key)
  const valueField = wrapper.find('[data-testid="rich-note-property-value"]')
  valueField.element.textContent = value
  await valueField.trigger("input")
  await wrapper
    .find('[data-testid="rich-note-property-insert-add"]')
    .trigger("click")
  await flushPromises()
}
