import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"
import type { VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import {
  PROPERTY_VALUE_DIALOG_OPEN_SELECTOR,
  propertyValueDialogEl,
  mountEditorOnNoteShow,
  PROPERTY_PANEL_NOTE_ID,
} from "./propertyValueDialogTestDom"
import {
  attemptRenamePropertyKey,
  collapsePropertyPanel,
  expandPropertyPanel,
  expandPropertyPanelAndClickRemove,
  expectPropertyPanelClosed,
  expectPropertyPanelOpen,
  propertyRowSelector,
} from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property location", () => {
  const h = createRichMarkdownEditorTestHarness()
  const noteId = PROPERTY_PANEL_NOTE_ID
  const topicRow = propertyRowSelector("topic")
  const conversationQuery = { conversation: "true" }
  const markdown = `---
topic: training
venue: online
---

Body.`

  beforeEach(() => {
    mockSdkService(NoteController, "getNoteInfo", { memoryTrackers: [] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
  })

  function spyOnRouter(wrapper: VueWrapper) {
    const router = wrapper.vm.$router
    return {
      router,
      replaceSpy: vi.spyOn(router, "replace"),
      pushSpy: vi.spyOn(router, "push"),
    }
  }

  function focusedFlag(wrapper: VueWrapper, key: string) {
    return wrapper
      .find(propertyRowSelector(key))
      .attributes("data-property-focused")
  }

  describe("visiting noteProperty", () => {
    it("focuses the row, scrolls it into view, and opens its property panel", async () => {
      const scrollSpy = vi.spyOn(HTMLElement.prototype, "scrollIntoView")

      const wrapper = await mountEditorOnNoteShow(h, markdown, {
        route: notePropertyLocation(noteId, "topic"),
      })

      const topic = wrapper.find(topicRow)
      expect(focusedFlag(wrapper, "topic")).toBe("true")
      expect(topic.classes()).toContain("bg-primary/10")
      expectPropertyPanelOpen(topic.element)
      expect(focusedFlag(wrapper, "venue")).toBeUndefined()
      expectPropertyPanelClosed(
        wrapper.find(propertyRowSelector("venue")).element
      )
      expect(scrollSpy).toHaveBeenCalledWith({
        behavior: "smooth",
        block: "center",
      })
    })

    it("on a read-only property focuses the row, scrolls it into view, and shows the value", async () => {
      const scrollSpy = vi.spyOn(HTMLElement.prototype, "scrollIntoView")

      const wrapper = await mountEditorOnNoteShow(h, markdown, {
        readonly: true,
        route: notePropertyLocation(noteId, "topic"),
      })

      const topic = wrapper.find(topicRow)
      expect(focusedFlag(wrapper, "topic")).toBe("true")
      expect(topic.classes()).toContain("bg-primary/10")
      expect(topic.text()).toContain("training")
      expect(focusedFlag(wrapper, "venue")).toBeUndefined()
      expect(scrollSpy).toHaveBeenCalledWith({
        behavior: "smooth",
        block: "center",
      })
    })

    it("on a specialized property focuses the row and opens its property panel", async () => {
      const specializedMarkdown = `---
image: https://example.com/workshop.png
topic: training
---

Workshop body.`

      const wrapper = await mountEditorOnNoteShow(h, specializedMarkdown, {
        route: notePropertyLocation(noteId, "image"),
      })

      const imageRow = wrapper.find(propertyRowSelector("image"))
      expect(imageRow.attributes("data-property-focused")).toBe("true")
      expectPropertyPanelOpen(imageRow.element)
      expect(
        imageRow
          .find('[data-testid="rich-note-image-property-choose"]')
          .exists()
      ).toBe(true)
    })

    it("for a missing key shows not-found with the decoded key", async () => {
      const wrapper = await mountEditorOnNoteShow(h, markdown, {
        route: notePropertyLocation(noteId, "example of"),
      })

      expect(
        wrapper.find('[data-testid="rich-note-property-not-found"]').text()
      ).toBe('Property "example of" not found')
      expect(focusedFlag(wrapper, "topic")).toBeUndefined()
    })
  })

  describe("property panel", () => {
    it("opening and closing the property panel replaces between noteShow and noteProperty, preserving query values", async () => {
      const wrapper = await mountEditorOnNoteShow(h, markdown, {
        route: { ...noteShowLocation(noteId), query: conversationQuery },
      })
      const { router, replaceSpy, pushSpy } = spyOnRouter(wrapper)

      await expandPropertyPanel(wrapper, topicRow)

      expect(replaceSpy).toHaveBeenCalledTimes(1)
      expect(router.currentRoute.value).toMatchObject(
        notePropertyLocation(noteId, "topic")
      )
      expect(router.currentRoute.value.query).toEqual(conversationQuery)
      expectPropertyPanelOpen(wrapper.find(topicRow).element)

      await collapsePropertyPanel(wrapper, topicRow)

      expect(replaceSpy).toHaveBeenCalledTimes(2)
      expect(router.currentRoute.value).toMatchObject(noteShowLocation(noteId))
      expect(router.currentRoute.value.query).toEqual(conversationQuery)
      expectPropertyPanelClosed(wrapper.find(topicRow).element)
      expect(pushSpy).not.toHaveBeenCalled()
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

  describe("renaming the focused property", () => {
    it("replaces to noteProperty with the new exact key, preserves query values, and keeps the property focused", async () => {
      const wrapper = await mountEditorOnNoteShow(h, markdown, {
        route: {
          ...notePropertyLocation(noteId, "topic"),
          query: conversationQuery,
        },
      })
      const { router, replaceSpy, pushSpy } = spyOnRouter(wrapper)

      await attemptRenamePropertyKey(wrapper, 0, "Subject Matter")

      expect(pushSpy).not.toHaveBeenCalled()
      expect(replaceSpy).toHaveBeenCalled()
      expect(router.currentRoute.value).toMatchObject(
        notePropertyLocation(noteId, "Subject Matter")
      )
      expect(router.currentRoute.value.query).toEqual(conversationQuery)
      const renamedRow = wrapper.find(propertyRowSelector("Subject Matter"))
      expect(renamedRow.attributes("data-property-focused")).toBe("true")
      expectPropertyPanelOpen(renamedRow.element)
    })

    it("does not leave noteShow when renaming a property that is not focused by the route", async () => {
      const wrapper = await mountEditorOnNoteShow(h, markdown)
      const { router, replaceSpy, pushSpy } = spyOnRouter(wrapper)

      await attemptRenamePropertyKey(wrapper, 0, "domain")

      expect(pushSpy).not.toHaveBeenCalled()
      expect(replaceSpy).not.toHaveBeenCalled()
      expect(router.currentRoute.value).toMatchObject(noteShowLocation(noteId))
    })
  })

  describe("deleting the focused property", () => {
    it("replaces to noteShow, preserves query, and does not show property-not-found", async () => {
      const wrapper = await mountEditorOnNoteShow(h, markdown, {
        route: {
          ...notePropertyLocation(noteId, "topic"),
          query: conversationQuery,
        },
      })
      const { router, replaceSpy, pushSpy } = spyOnRouter(wrapper)

      await expandPropertyPanelAndClickRemove(
        wrapper,
        propertyRowSelector("topic")
      )

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
            `${propertyRowSelector("venue")} [data-testid="rich-note-property-row-remove"]`
          )
          .exists()
      ).toBe(false)
      expect(wrapper.vm.$router.currentRoute.value).toMatchObject(
        notePropertyLocation(noteId, "topic")
      )
    })
  })
})
