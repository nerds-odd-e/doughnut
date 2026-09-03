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
  PROPERTY_VALUE_DIALOG_OPEN_SELECTOR,
  propertyValueDialogEl,
  mountEditorOnNoteShow,
  PROPERTY_PANEL_NOTE_ID,
} from "./propertyValueDialogTestDom"
import {
  collapsePropertyPanel,
  expandPropertyPanel,
  expectPropertyPanelClosed,
  expectPropertyPanelOpen,
  propertyRowSelector,
} from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property panel location", () => {
  const h = createRichMarkdownEditorTestHarness()
  const noteId = PROPERTY_PANEL_NOTE_ID
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

    await expandPropertyPanel(wrapper, topicRow)

    expect(pushSpy).not.toHaveBeenCalled()
    expect(replaceSpy).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value).toMatchObject(
      notePropertyLocation(noteId, "topic")
    )
    expectPropertyPanelOpen(wrapper.find(topicRow).element)
  })

  it("closing the property panel replaces to noteShow", async () => {
    const wrapper = await mountEditorOnNoteShow(h, markdown, {
      route: notePropertyLocation(noteId, "topic"),
    })
    expectPropertyPanelOpen(wrapper.find(topicRow).element)
    const router = editorRouter(wrapper)
    const replaceSpy = vi.spyOn(router, "replace")
    const pushSpy = vi.spyOn(router, "push")

    await collapsePropertyPanel(wrapper, topicRow)

    expect(pushSpy).not.toHaveBeenCalled()
    expect(replaceSpy).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value).toMatchObject(noteShowLocation(noteId))
    expectPropertyPanelClosed(wrapper.find(topicRow).element)
  })

  it("preserves unrelated query values when opening and closing the property panel", async () => {
    const conversationQuery = { conversation: "true" }
    const wrapper = await mountEditorOnNoteShow(h, markdown, {
      route: { ...noteShowLocation(noteId), query: conversationQuery },
    })
    const router = editorRouter(wrapper)
    const replaceSpy = vi.spyOn(router, "replace")

    await expandPropertyPanel(wrapper, topicRow)

    expect(replaceSpy).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value).toMatchObject(
      notePropertyLocation(noteId, "topic")
    )
    expect(router.currentRoute.value.query).toEqual(conversationQuery)

    await collapsePropertyPanel(wrapper, topicRow)

    expect(replaceSpy).toHaveBeenCalledTimes(2)
    expect(router.currentRoute.value).toMatchObject(noteShowLocation(noteId))
    expect(router.currentRoute.value.query).toEqual(conversationQuery)
  })

  it("opening the property value dialog from its control leaves the property panel closed", async () => {
    const wrapper = await mountEditorOnNoteShow(h, markdown)
    const openButton = wrapper.find(PROPERTY_VALUE_DIALOG_OPEN_SELECTOR)
    expect(openButton.exists()).toBe(true)

    await openButton.trigger("click")

    expectPropertyPanelClosed(wrapper.find(topicRow).element)
    expect(propertyValueDialogEl()).not.toBeNull()
  })
})
