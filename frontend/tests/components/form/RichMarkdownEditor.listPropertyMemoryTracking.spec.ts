import {
  AssimilationController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { expandPropertyPanel, propertyRowSelector } from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

vi.mock("@/composables/useGoToNextAssimilation", () => ({
  useGoToNextAssimilation: () => ({
    goToNextAssimilation: vi.fn().mockResolvedValue(true),
  }),
}))

describe("RichMarkdownEditor list property memory tracking", () => {
  const h = createRichMarkdownEditorTestHarness()
  const noteId = 42
  const listMarkdown = `---
example of:
  - "[[run]]"
  - "[[past tense]]"
---

Workshop body.`
  const listRowSelector = propertyRowSelector("example of")
  const assimilateValueSelector = (value: string) =>
    `${listRowSelector} button[aria-label="Assimilate ${value}"]`
  const valueStatusSelector = (value: string) =>
    `${listRowSelector} [data-property-value="${value}"] [data-test="assimilation-status-UNDERSTANDING"]`

  let getNoteInfoSpy: ReturnType<typeof mockSdkService>

  const mountListEditor = async () => {
    const wrapper = await h.mountEditor(listMarkdown, {
      noteId,
      route: noteShowLocation(noteId),
    })
    await expandPropertyPanel(wrapper, listRowSelector)
    return wrapper
  }

  beforeEach(() => {
    getNoteInfoSpy = mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [],
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
  })

  it("offers one Assimilate per value and one Skip for the key", async () => {
    const wrapper = await mountListEditor()

    expect(
      wrapper
        .findAll(`${listRowSelector} [data-test="assimilate-UNDERSTANDING"]`)
        .map((b) => b.attributes("aria-label"))
    ).toEqual(["Assimilate [[run]]", "Assimilate [[past tense]]"])
    expect(
      wrapper.find(`${listRowSelector} [data-property-value="[[run]]"]`).text()
    ).toContain("[[run]]")
    expect(
      wrapper.findAll(`${listRowSelector} [data-test="skip"]`)
    ).toHaveLength(1)
  })

  it("assimilates one value and keeps the other offered", async () => {
    const runTracker = makeMe.aMemoryTracker
      .id(1)
      .withPropertyKey("example of", "[[run]]")
      .please()
    const assimilateSpy = mockSdkService(AssimilationController, "assimilate", [
      runTracker,
    ])
    const wrapper = await mountListEditor()
    getNoteInfoSpy.mockResolvedValue(
      wrapSdkResponse(
        makeMe.aNoteRecallInfo.memoryTrackers([runTracker]).please()
      )
    )

    await wrapper.find(assimilateValueSelector("[[run]]")).trigger("click")
    await flushPromises()

    expect(assimilateSpy).toHaveBeenCalledWith({
      body: { noteId, propertyKey: "example of", propertyValue: "[[run]]" },
    })
    expect(wrapper.find(valueStatusSelector("[[run]]")).exists()).toBe(true)
    expect(wrapper.find(assimilateValueSelector("[[run]]")).exists()).toBe(
      false
    )
    expect(
      wrapper.find(assimilateValueSelector("[[past tense]]")).exists()
    ).toBe(true)
  })
})
