import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import {
  mountEditorOnNoteShow,
  PROPERTY_PANEL_NOTE_ID,
} from "./propertyValueDialogTestDom"
import { attemptRemovePropertyRow } from "./propertiesTestSupport"
import { propertyRowSelector } from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor focused property delete location", () => {
  const h = createRichMarkdownEditorTestHarness()
  const noteId = PROPERTY_PANEL_NOTE_ID
  const markdown = `---
topic: training
subject: other
---

Body.`

  beforeEach(() => {
    mockSdkService(NoteController, "getNoteInfo", { memoryTrackers: [] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
  })

  it("replaces to noteShow, preserves query, and does not show property-not-found after deleting the focused property", async () => {
    const conversationQuery = { conversation: "true" }
    const wrapper = await mountEditorOnNoteShow(h, markdown, {
      route: {
        ...notePropertyLocation(noteId, "topic"),
        query: conversationQuery,
      },
    })
    const router = wrapper.vm.$router
    const replaceSpy = vi.spyOn(router, "replace")
    const pushSpy = vi.spyOn(router, "push")

    await attemptRemovePropertyRow(wrapper, "topic")

    expect(pushSpy).not.toHaveBeenCalled()
    expect(replaceSpy).toHaveBeenCalled()
    expect(router.currentRoute.value).toMatchObject(noteShowLocation(noteId))
    expect(router.currentRoute.value.query).toEqual(conversationQuery)
    expect(
      wrapper.find('[data-testid="rich-note-property-not-found"]').exists()
    ).toBe(false)
  })

  it("does not expose remove for a property that is not focused", async () => {
    const wrapper = await mountEditorOnNoteShow(h, markdown, {
      route: notePropertyLocation(noteId, "topic"),
    })

    expect(
      wrapper
        .find(
          `${propertyRowSelector("subject")} [data-testid="rich-note-property-row-remove"]`
        )
        .exists()
    ).toBe(false)
    expect(wrapper.vm.$router.currentRoute.value).toMatchObject(
      notePropertyLocation(noteId, "topic")
    )
  })
})
