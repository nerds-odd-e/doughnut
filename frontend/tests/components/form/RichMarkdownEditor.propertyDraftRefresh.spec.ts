import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, describe, expect, it, vi } from "vitest"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property draft refresh", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
  })

  it("retains the rename and newer value while its guard waits across a body refresh", async () => {
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
    const key = wrapper.find('[data-testid="rich-note-property-row-key-input"]')
    await key.trigger("focus")
    await key.setValue("domain")
    await key.trigger("blur")
    expect(getNoteInfo).toHaveBeenCalled()
    await h.setPropertyValueField(
      wrapper.find('[data-testid="rich-note-property-row-value-input"]'),
      "newer value"
    )
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
