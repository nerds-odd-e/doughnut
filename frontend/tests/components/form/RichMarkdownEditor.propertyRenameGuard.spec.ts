import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { attemptRenamePropertyKey } from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const ROW_VALUE_INPUT = '[data-testid="rich-note-property-row-value-input"]'

describe("RichMarkdownEditor property rename guard", () => {
  const h = createRichMarkdownEditorTestHarness()

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ["requestAnimationFrame"] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
    vi.useRealTimers()
  })

  it("retains a rename and newer value while its guard waits across a body refresh", async () => {
    const response = wrapSdkResponse({ memoryTrackers: [] })
    let resolveNoteInfo!: (value: typeof response) => void
    const noteInfo = new Promise<typeof response>((resolve) => {
      resolveNoteInfo = resolve
    })
    const getNoteInfo = mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [],
    }).mockReturnValue(noteInfo)
    const wrapper = await h.mountEditor("---\ntopic: wiki\n---\n\nBody.", {
      noteId: 42,
      route: noteShowLocation(42),
    })
    await attemptRenamePropertyKey(wrapper, 0, "domain")
    expect(getNoteInfo).toHaveBeenCalled()
    await h.setPropertyValueField(wrapper.find(ROW_VALUE_INPUT), "newer value")
    await wrapper.setProps({
      modelValue: '---\ntopic: "wiki"\n---\n\nRefreshed body.',
    })
    expect(h.quillEditorEl().textContent).toContain("Refreshed body.")
    resolveNoteInfo(response)
    await flushPromises()
    expect(h.lastEmittedMarkdown()).toBe(
      "---\ndomain: newer value\n---\n\nRefreshed body."
    )
  })
})
