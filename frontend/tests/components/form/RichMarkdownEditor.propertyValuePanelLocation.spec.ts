import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, describe, expect, it, vi } from "vitest"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import type { Router } from "vue-router"
import {
  clickCancel,
  dialogEl,
  mountEditorOnNoteShow,
  openValuePopup,
  PROPERTY_VALUE_PANEL_NOTE_ID,
} from "./propertyValuePopupTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property value panel location", () => {
  const h = createRichMarkdownEditorTestHarness()
  const noteId = PROPERTY_VALUE_PANEL_NOTE_ID
  const markdown = `---
topic: training
---

Body.`

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
  })

  function editorRouter(wrapper: VueWrapper): Router {
    return wrapper.vm.$router
  }

  it("opening the value panel replaces to noteProperty and opens the dialog", async () => {
    const wrapper = await mountEditorOnNoteShow(h, markdown)
    const router = editorRouter(wrapper)
    const replaceSpy = vi.spyOn(router, "replace")
    const pushSpy = vi.spyOn(router, "push")

    await openValuePopup(wrapper)

    expect(pushSpy).not.toHaveBeenCalled()
    expect(replaceSpy).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value).toMatchObject(
      notePropertyLocation(noteId, "topic")
    )
    expect(dialogEl()).not.toBeNull()
    expect(document.querySelector("dialog h2")?.textContent).toBe("topic")
  })

  it("closing the value panel replaces to noteShow and closes the dialog", async () => {
    const wrapper = await mountEditorOnNoteShow(h, markdown, {
      route: notePropertyLocation(noteId, "topic"),
    })
    expect(dialogEl()).not.toBeNull()
    const router = editorRouter(wrapper)
    const replaceSpy = vi.spyOn(router, "replace")
    const pushSpy = vi.spyOn(router, "push")

    clickCancel()
    await flushPromises()

    expect(pushSpy).not.toHaveBeenCalled()
    expect(replaceSpy).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value).toMatchObject(noteShowLocation(noteId))
    expect(dialogEl()).toBeNull()
  })

  it("preserves unrelated query values when opening and closing the value panel", async () => {
    const conversationQuery = { conversation: "true" }
    const wrapper = await mountEditorOnNoteShow(h, markdown, {
      route: { ...noteShowLocation(noteId), query: conversationQuery },
    })
    const router = editorRouter(wrapper)
    const replaceSpy = vi.spyOn(router, "replace")

    await openValuePopup(wrapper)

    expect(replaceSpy).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value).toMatchObject(
      notePropertyLocation(noteId, "topic")
    )
    expect(router.currentRoute.value.query).toEqual(conversationQuery)

    clickCancel()
    await flushPromises()

    expect(replaceSpy).toHaveBeenCalledTimes(2)
    expect(router.currentRoute.value).toMatchObject(noteShowLocation(noteId))
    expect(router.currentRoute.value.query).toEqual(conversationQuery)
  })
})
