import {
  AssimilationController,
  AssimilationSequenceSkipController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  expandPropertyRowOptions,
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

describe("RichMarkdownEditor property assimilation controls", () => {
  const h = createRichMarkdownEditorTestHarness()
  const noteId = 42
  const topicMarkdown = `---
topic: training
---

Workshop body.`

  const topicRowSelector = propertyRowSelector("topic")

  beforeEach(() => {
    mockSdkService(NoteController, "getNoteInfo", { memoryTrackers: [] })
    confirmMock.mockReset()
    mockedGoToNextAssimilation.mockReset()
    mockedGoToNextAssimilation.mockResolvedValue(true)
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
  })

  it("assimilates the property from its own toggle-options row", async () => {
    const assimilateSpy = mockSdkService(AssimilationController, "assimilate", [
      makeMe.aMemoryTracker.id(1).withPropertyKey("topic").please(),
    ])

    const wrapper = await h.mountEditor(topicMarkdown, { noteId })
    await expandPropertyRowOptions(wrapper, topicRowSelector)

    await wrapper
      .find(`${topicRowSelector} [data-test="assimilate"]`)
      .trigger("click")
    await flushPromises()

    expect(assimilateSpy).toHaveBeenCalledWith({
      body: { noteId, propertyKey: "topic" },
    })
    expect(mockedGoToNextAssimilation).toHaveBeenCalled()
  })

  it("skips the property from its own toggle-options row after confirming", async () => {
    const skipSpy = mockSdkService(
      AssimilationSequenceSkipController,
      "create",
      {
        id: 1,
      }
    )
    confirmMock.mockImplementationOnce(() => Promise.resolve(true))

    const wrapper = await h.mountEditor(topicMarkdown, { noteId })
    await expandPropertyRowOptions(wrapper, topicRowSelector)

    await wrapper
      .find(`${topicRowSelector} [data-test="skip"]`)
      .trigger("click")
    await flushPromises()

    expect(skipSpy).toHaveBeenCalledWith({
      body: { noteId, propertyKey: "topic" },
    })
  })
})
