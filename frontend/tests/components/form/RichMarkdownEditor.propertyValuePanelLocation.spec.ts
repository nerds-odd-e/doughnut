import { type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import type { Router } from "vue-router"
import {
  dialogEl,
  mountEditorOnNoteShow,
  openValuePopup,
  PROPERTY_VALUE_PANEL_NOTE_ID,
} from "./propertyValuePopupTestDom"
import {
  collapsePropertyRowOptions,
  expandPropertyRowOptions,
  expectPropertyRowPanelClosed,
  expectPropertyRowPanelOpen,
  propertyRowSelector,
} from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property panel location", () => {
  const h = createRichMarkdownEditorTestHarness()
  const noteId = PROPERTY_VALUE_PANEL_NOTE_ID
  const topicRow = propertyRowSelector("topic")
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

  function editorRouter(wrapper: VueWrapper): Router {
    return wrapper.vm.$router
  }

  it("opening the property panel replaces to noteProperty and opens the panel", async () => {
    const wrapper = await mountEditorOnNoteShow(h, markdown)
    const router = editorRouter(wrapper)
    const replaceSpy = vi.spyOn(router, "replace")
    const pushSpy = vi.spyOn(router, "push")

    await expandPropertyRowOptions(wrapper, topicRow)

    expect(pushSpy).not.toHaveBeenCalled()
    expect(replaceSpy).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value).toMatchObject(
      notePropertyLocation(noteId, "topic")
    )
    expectPropertyRowPanelOpen(wrapper.find(topicRow).element)
  })

  it("closing the property panel replaces to noteShow", async () => {
    const wrapper = await mountEditorOnNoteShow(h, markdown, {
      route: notePropertyLocation(noteId, "topic"),
    })
    expectPropertyRowPanelOpen(wrapper.find(topicRow).element)
    const router = editorRouter(wrapper)
    const replaceSpy = vi.spyOn(router, "replace")
    const pushSpy = vi.spyOn(router, "push")

    await collapsePropertyRowOptions(wrapper, topicRow)

    expect(pushSpy).not.toHaveBeenCalled()
    expect(replaceSpy).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value).toMatchObject(noteShowLocation(noteId))
    expectPropertyRowPanelClosed(wrapper.find(topicRow).element)
  })

  it("preserves unrelated query values when opening and closing the property panel", async () => {
    const conversationQuery = { conversation: "true" }
    const wrapper = await mountEditorOnNoteShow(h, markdown, {
      route: { ...noteShowLocation(noteId), query: conversationQuery },
    })
    const router = editorRouter(wrapper)
    const replaceSpy = vi.spyOn(router, "replace")

    await expandPropertyRowOptions(wrapper, topicRow)

    expect(replaceSpy).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value).toMatchObject(
      notePropertyLocation(noteId, "topic")
    )
    expect(router.currentRoute.value.query).toEqual(conversationQuery)

    await collapsePropertyRowOptions(wrapper, topicRow)

    expect(replaceSpy).toHaveBeenCalledTimes(2)
    expect(router.currentRoute.value).toMatchObject(noteShowLocation(noteId))
    expect(router.currentRoute.value.query).toEqual(conversationQuery)
  })

  it("opening the property value dialog from its control leaves the property panel closed", async () => {
    const wrapper = await mountEditorOnNoteShow(h, markdown)

    await openValuePopup(wrapper)

    expectPropertyRowPanelClosed(wrapper.find(topicRow).element)
    expect(dialogEl()).not.toBeNull()
  })
})
