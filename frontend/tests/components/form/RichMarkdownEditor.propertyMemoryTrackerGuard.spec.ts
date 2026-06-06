import {
  MemoryTrackerController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
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
  let softDeleteSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    getNoteInfoSpy = mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [],
    })
    softDeleteSpy = mockSdkService(
      MemoryTrackerController,
      "softDelete",
      undefined
    )
    confirmMock.mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
  })

  function mockNoteInfoWithPropertyTracker(key: string, id: number) {
    const tracker = makeMe.aMemoryTracker.please()
    tracker.id = id
    tracker.propertyKey = key
    getNoteInfoSpy.mockResolvedValue(
      wrapSdkResponse(makeMe.aNoteRecallInfo.memoryTrackers([tracker]).please())
    )
    return tracker
  }

  const topicRowRemoveSelector =
    '[data-testid="rich-note-property-row"][data-property-key="topic"] [data-testid="rich-note-property-row-remove"]'

  it("soft-deletes the tracker and removes the property when the user confirms", async () => {
    const tracker = mockNoteInfoWithPropertyTracker("topic", 99)
    confirmMock.mockImplementationOnce(() => Promise.resolve(true))

    const wrapper = await h.mountEditor(trackedPropertyMarkdown, { noteId })
    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0

    await wrapper.find(topicRowRemoveSelector).trigger("click")
    await flushPromises()

    await vi.waitFor(() => {
      expect(softDeleteSpy).toHaveBeenCalledWith({
        path: { memoryTracker: tracker.id },
      })
    })

    expect(confirmMock).toHaveBeenCalledWith(
      'Property "topic" has a memory tracker. Deleting it will also delete that tracker. Continue?'
    )
    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBeGreaterThan(
      emitCountBefore
    )
    const last = h.lastEmittedMarkdown()
    expect(last).not.toContain("topic:")
    expect(last).toContain("Workshop body")
    expect(
      wrapper
        .find(
          '[data-testid="rich-note-property-row"][data-property-key="topic"]'
        )
        .exists()
    ).toBe(false)
  })

  it("keeps the property row and does not emit when the user cancels", async () => {
    mockNoteInfoWithPropertyTracker("topic", 99)
    confirmMock.mockImplementationOnce(() => Promise.resolve(false))

    const wrapper = await h.mountEditor(trackedPropertyMarkdown, { noteId })
    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0

    await wrapper.find(topicRowRemoveSelector).trigger("click")
    await flushPromises()

    await vi.waitFor(() => {
      expect(confirmMock).toHaveBeenCalledOnce()
    })

    expect(softDeleteSpy).not.toHaveBeenCalled()
    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      emitCountBefore
    )
    expect(
      wrapper
        .find(
          '[data-testid="rich-note-property-row"][data-property-key="topic"]'
        )
        .exists()
    ).toBe(true)
  })
})
