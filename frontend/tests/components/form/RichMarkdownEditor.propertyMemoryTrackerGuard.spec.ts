import {
  MemoryTrackerController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { noteShowLocation } from "@/routes/noteShowLocation"
import {
  expandPropertyPanelAndClickRemove,
  propertyRowSelector,
} from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const confirmMock = vi.fn()

vi.mock("@/components/commons/Popups/usePopups", () => ({
  default: () => ({
    popups: {
      confirm: confirmMock,
      alert: vi.fn(),
      options: vi.fn(),
      done: vi.fn(),
      register: vi.fn(),
      peek: vi.fn(),
    },
  }),
}))

describe("RichMarkdownEditor property memory tracker guard", () => {
  const h = createRichMarkdownEditorTestHarness()
  const noteId = 42
  const trackedPropertyMarkdown = `---
topic: training
---

Workshop body.`

  let getNoteInfoSpy: ReturnType<typeof mockSdkService>
  let deleteSpy: ReturnType<typeof mockSdkService>
  let updatePropertyKeySpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    getNoteInfoSpy = mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [],
    })
    deleteSpy = mockSdkService(MemoryTrackerController, "delete", undefined)
    updatePropertyKeySpy = mockSdkService(
      MemoryTrackerController,
      "updatePropertyKey",
      undefined
    )
    confirmMock.mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
  })

  function mockNoteInfoWithPropertyTracker(key: string, id: number) {
    const tracker = makeMe.aMemoryTracker.id(id).withPropertyKey(key).please()
    getNoteInfoSpy.mockResolvedValue(
      wrapSdkResponse(makeMe.aNoteRecallInfo.memoryTrackers([tracker]).please())
    )
    return tracker
  }

  const topicRowSelector = propertyRowSelector("topic")
  const topicRowKeyInputSelector = `${topicRowSelector} [data-testid="rich-note-property-row-key-input"]`

  it("hard-deletes the tracker and removes the property when the user confirms", async () => {
    const tracker = mockNoteInfoWithPropertyTracker("topic", 99)
    confirmMock.mockImplementationOnce(() => Promise.resolve(true))

    const wrapper = await h.mountEditor(trackedPropertyMarkdown, {
      noteId,
      route: noteShowLocation(noteId),
    })

    await expandPropertyPanelAndClickRemove(wrapper, topicRowSelector)
    await flushPromises()

    await vi.waitFor(() => {
      expect(deleteSpy).toHaveBeenCalledWith({
        path: { memoryTracker: tracker.id },
      })
    })

    expect(h.lastEmittedMarkdown()).not.toContain("topic:")
    expect(wrapper.find(topicRowSelector).exists()).toBe(false)
  })

  it("keeps the property row and does not emit when the user cancels", async () => {
    mockNoteInfoWithPropertyTracker("topic", 99)
    confirmMock.mockImplementationOnce(() => Promise.resolve(false))

    const wrapper = await h.mountEditor(trackedPropertyMarkdown, {
      noteId,
    })
    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0

    await expandPropertyPanelAndClickRemove(wrapper, topicRowSelector)

    expect(confirmMock).toHaveBeenCalledOnce()
    expect(deleteSpy).not.toHaveBeenCalled()
    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      emitCountBefore
    )
    expect(wrapper.find(topicRowSelector).exists()).toBe(true)
  })

  it("reverts the property key and does not emit when the user cancels a rename", async () => {
    mockNoteInfoWithPropertyTracker("topic", 99)
    confirmMock.mockImplementationOnce(() => Promise.resolve(false))

    const wrapper = await h.mountEditor(trackedPropertyMarkdown, {
      noteId,
      route: noteShowLocation(noteId),
    })
    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0
    const keyInput = wrapper.find(topicRowKeyInputSelector)

    await keyInput.trigger("focus")
    await keyInput.setValue("subject")
    await keyInput.trigger("blur")
    await flushPromises()

    await vi.waitFor(() => {
      expect(confirmMock).toHaveBeenCalledOnce()
    })

    expect(updatePropertyKeySpy).not.toHaveBeenCalled()
    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      emitCountBefore
    )
    expect(keyInput.element).toHaveValue("topic")
  })
})
