import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import {
  mountEditorOnNoteShow,
  PROPERTY_VALUE_PANEL_NOTE_ID,
} from "./propertyValuePopupTestDom"
import { attemptRenamePropertyKey } from "./propertiesTestSupport"
import {
  expectPropertyRowPanelOpen,
  propertyRowSelector,
} from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor focused property rename location", () => {
  const h = createRichMarkdownEditorTestHarness()
  const noteId = PROPERTY_VALUE_PANEL_NOTE_ID
  const markdown = `---
topic: training
---

Body.`

  beforeEach(() => {
    mockSdkService(NoteController, "getNoteInfo", { memoryTrackers: [] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
  })

  it("replaces to noteProperty with the new exact key and keeps the property focused", async () => {
    const wrapper = await mountEditorOnNoteShow(h, markdown, {
      route: notePropertyLocation(noteId, "topic"),
    })
    const router = wrapper.vm.$router
    const replaceSpy = vi.spyOn(router, "replace")
    const pushSpy = vi.spyOn(router, "push")

    await attemptRenamePropertyKey(wrapper, 0, "Subject Matter")

    expect(pushSpy).not.toHaveBeenCalled()
    expect(replaceSpy).toHaveBeenCalled()
    expect(router.currentRoute.value).toMatchObject(
      notePropertyLocation(noteId, "Subject Matter")
    )
    expect(
      wrapper
        .find(propertyRowSelector("Subject Matter"))
        .attributes("data-property-focused")
    ).toBe("true")
    expectPropertyRowPanelOpen(
      wrapper.find(propertyRowSelector("Subject Matter")).element
    )
  })

  it("preserves unrelated query values when the focused property key is renamed", async () => {
    const conversationQuery = { conversation: "true" }
    const wrapper = await mountEditorOnNoteShow(h, markdown, {
      route: {
        ...notePropertyLocation(noteId, "topic"),
        query: conversationQuery,
      },
    })
    const router = wrapper.vm.$router

    await attemptRenamePropertyKey(wrapper, 0, "subject")

    expect(router.currentRoute.value).toMatchObject(
      notePropertyLocation(noteId, "subject")
    )
    expect(router.currentRoute.value.query).toEqual(conversationQuery)
  })

  it("does not leave noteShow when renaming a property that is not focused by the route", async () => {
    const wrapper = await mountEditorOnNoteShow(h, markdown)
    const router = wrapper.vm.$router
    const replaceSpy = vi.spyOn(router, "replace")
    const pushSpy = vi.spyOn(router, "push")

    await attemptRenamePropertyKey(wrapper, 0, "subject")

    expect(pushSpy).not.toHaveBeenCalled()
    expect(replaceSpy).not.toHaveBeenCalled()
    expect(router.currentRoute.value).toMatchObject(noteShowLocation(noteId))
  })
})
