import {
  AssimilationController,
  AssimilationSequenceSkipController,
  MemoryTrackerController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { noteShowLocation } from "@/routes/noteShowLocation"
import {
  expandPropertyPanel,
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

const mockedGoToNextAssimilation = vi.fn()

vi.mock("@/composables/useGoToNextAssimilation", () => ({
  useGoToNextAssimilation: () => ({
    goToNextAssimilation: mockedGoToNextAssimilation,
  }),
}))

describe("RichMarkdownEditor property memory tracking", () => {
  const h = createRichMarkdownEditorTestHarness()
  const noteId = 42
  const topicMarkdown = `---
topic: training
---

Workshop body.`
  const topicRowSelector = propertyRowSelector("topic")

  let getNoteInfoSpy: ReturnType<typeof mockSdkService>

  const mountTopicEditor = (markdown = topicMarkdown) =>
    h.mountEditor(markdown, { noteId, route: noteShowLocation(noteId) })

  beforeEach(() => {
    getNoteInfoSpy = mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [],
    })
    confirmMock.mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
  })

  describe("assimilation controls", () => {
    beforeEach(() => {
      mockedGoToNextAssimilation.mockReset()
      mockedGoToNextAssimilation.mockResolvedValue(true)
    })

    it("assimilates the property from its own property panel", async () => {
      const assimilateSpy = mockSdkService(
        AssimilationController,
        "assimilate",
        [makeMe.aMemoryTracker.id(1).withPropertyKey("topic").please()]
      )

      const wrapper = await mountTopicEditor()
      await expandPropertyPanel(wrapper, topicRowSelector)

      await wrapper
        .find(`${topicRowSelector} [data-test="assimilate-UNDERSTANDING"]`)
        .trigger("click")
      await flushPromises()

      expect(assimilateSpy).toHaveBeenCalledWith({
        body: { noteId, propertyKey: "topic" },
      })
      expect(mockedGoToNextAssimilation).toHaveBeenCalled()
    })

    it("hides assimilation buttons on the note_level property row", async () => {
      const markdown = `---
note_level: 2
---

Workshop body.`
      const noteLevelRowSelector = propertyRowSelector("note_level")

      const wrapper = await mountTopicEditor(markdown)
      await expandPropertyPanel(wrapper, noteLevelRowSelector)

      expect(
        wrapper
          .find(
            `${noteLevelRowSelector} [data-test="assimilate-UNDERSTANDING"]`
          )
          .exists()
      ).toBe(false)
      expect(
        wrapper.find(`${noteLevelRowSelector} [data-test="skip"]`).exists()
      ).toBe(false)
    })

    it("shows a linked status instead of assimilate/skip when the property has an active tracker", async () => {
      mockSdkService(NoteController, "getNoteInfo", {
        memoryTrackers: [
          makeMe.aMemoryTracker
            .id(7)
            .withPropertyKey("topic")
            .nextRecallAt("2026-09-12T10:00:00.000Z")
            .please(),
        ],
      })

      const wrapper = await mountTopicEditor()
      await expandPropertyPanel(wrapper, topicRowSelector)

      expect(
        wrapper
          .find(
            `${topicRowSelector} [data-test="assimilation-status-UNDERSTANDING"]`
          )
          .exists()
      ).toBe(true)
      expect(
        wrapper
          .find(`${topicRowSelector} [data-test="assimilate-UNDERSTANDING"]`)
          .exists()
      ).toBe(false)
      expect(
        wrapper.find(`${topicRowSelector} [data-test="skip"]`).exists()
      ).toBe(false)
    })

    it("skips the property from its own property panel after confirming", async () => {
      const skipSpy = mockSdkService(
        AssimilationSequenceSkipController,
        "create",
        { id: 1 }
      )
      confirmMock.mockImplementationOnce(() => Promise.resolve(true))

      const wrapper = await mountTopicEditor()
      await expandPropertyPanel(wrapper, topicRowSelector)

      await wrapper
        .find(`${topicRowSelector} [data-test="skip"]`)
        .trigger("click")
      await flushPromises()

      expect(skipSpy).toHaveBeenCalledWith({
        body: { noteId, propertyKey: "topic" },
      })
    })
  })

  describe("guarding a tracked property", () => {
    const topicRowKeyInputSelector = `${topicRowSelector} [data-testid="rich-note-property-row-key-input"]`

    let deleteSpy: ReturnType<typeof mockSdkService>
    let updatePropertyKeySpy: ReturnType<typeof mockSdkService>

    beforeEach(() => {
      deleteSpy = mockSdkService(MemoryTrackerController, "delete", undefined)
      updatePropertyKeySpy = mockSdkService(
        MemoryTrackerController,
        "updatePropertyKey",
        undefined
      )
    })

    function mockNoteInfoWithPropertyTracker(key: string, id: number) {
      const tracker = makeMe.aMemoryTracker.id(id).withPropertyKey(key).please()
      getNoteInfoSpy.mockResolvedValue(
        wrapSdkResponse(
          makeMe.aNoteRecallInfo.memoryTrackers([tracker]).please()
        )
      )
      return tracker
    }

    it("hard-deletes the tracker and removes the property when the user confirms", async () => {
      const tracker = mockNoteInfoWithPropertyTracker("topic", 99)
      confirmMock.mockImplementationOnce(() => Promise.resolve(true))

      const wrapper = await mountTopicEditor()

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

    it("reverts a canceled rename, then keeps the property after a canceled removal without emitting", async () => {
      mockNoteInfoWithPropertyTracker("topic", 99)
      confirmMock.mockResolvedValue(false)

      const wrapper = await mountTopicEditor()
      const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0
      const keyInput = wrapper.find(topicRowKeyInputSelector)

      await keyInput.trigger("focus")
      await keyInput.setValue("subject")
      await keyInput.trigger("blur")
      await flushPromises()

      expect(confirmMock).toHaveBeenCalledOnce()
      expect(updatePropertyKeySpy).not.toHaveBeenCalled()
      expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
        emitCountBefore
      )
      expect(keyInput.element).toHaveValue("topic")

      await expandPropertyPanelAndClickRemove(wrapper, topicRowSelector)

      expect(confirmMock).toHaveBeenCalledTimes(2)
      expect(deleteSpy).not.toHaveBeenCalled()
      expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
        emitCountBefore
      )
      expect(wrapper.find(topicRowSelector).exists()).toBe(true)
    })
  })
})
