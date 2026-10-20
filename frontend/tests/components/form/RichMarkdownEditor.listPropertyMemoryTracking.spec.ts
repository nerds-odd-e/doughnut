import {
  AssimilationController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { wikiLinkFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import { expandPropertyPanel, propertyRowSelector } from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

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
    ).toContain("run")
    expect(
      wrapper.find(`${listRowSelector} [data-property-value="[[run]]"]`).text()
    ).not.toContain("[[")
    expect(
      wrapper.findAll(`${listRowSelector} [data-test="skip"]`)
    ).toHaveLength(1)
  })

  it("reads the complete authored list in order in detail, with and without a saved note", async () => {
    const longValue = "x".repeat(260)
    const authoredMarkdown = `---
url:
  - "[[Grammar]]"
  - "https://example.com/path"
  - "practice"
  - "practice"
  - ""
  - "${longValue}"
---

Body`
    const expectedSequence = [
      "Grammar",
      "https://example.com/path",
      "practice",
      "practice",
      "",
      longValue,
    ]
    const urlRowSelector = propertyRowSelector("url")
    const detailedRows = (wrapper: Awaited<ReturnType<typeof h.mountEditor>>) =>
      wrapper.findAll(
        `${urlRowSelector} [data-testid="rich-note-property-panel"] [data-property-value]`
      )
    const detailedLabels = (
      wrapper: Awaited<ReturnType<typeof h.mountEditor>>
    ) =>
      detailedRows(wrapper).map((row) =>
        row.find('[data-test="mode-label-UNDERSTANDING"]').text().trim()
      )
    const practiceTracker = makeMe.aMemoryTracker
      .id(77)
      .withPropertyKey("url", "practice")
      .please()
    getNoteInfoSpy.mockResolvedValue(
      wrapSdkResponse(
        makeMe.aNoteRecallInfo.memoryTrackers([practiceTracker]).please()
      )
    )
    const wrapper = await h.mountEditor(authoredMarkdown, {
      noteId,
      route: noteShowLocation(noteId),
      wikiLinks: [wikiLinkFromAuthoredToken("Grammar", 42)],
      lastSavedMarkdown: authoredMarkdown,
    })
    await expandPropertyPanel(wrapper, urlRowSelector)

    const rows = detailedRows(wrapper)
    expect(detailedLabels(wrapper)).toEqual(expectedSequence)
    expect(
      rows[1]!
        .find('[data-testid="rich-note-property-external-link"]')
        .attributes("aria-label")
    ).toBe("Open URL in new tab")
    const practiceTrackerLocations = [rows[2]!, rows[3]!].map((row) =>
      row
        .find('[data-test="assimilation-status-UNDERSTANDING"]')
        .attributes("href")
    )
    expect(new Set(practiceTrackerLocations).size).toBe(1)
    expect(practiceTrackerLocations[0]).toContain("77")
    expect(
      wrapper
        .findAll(
          `${urlRowSelector} [data-testid="rich-note-property-panel"] [data-test="assimilate-UNDERSTANDING"]`
        )
        .map((b) => b.attributes("aria-label"))
    ).toEqual(["Assimilate [[Grammar]]", "Assimilate https://example.com/path"])
    expect(
      wrapper.findAll(
        `${urlRowSelector} [data-testid="rich-note-property-panel"] [data-test="skip"]`
      )
    ).toHaveLength(1)

    const unsavedWrapper = await h.mountEditor(authoredMarkdown, {
      route: noteShowLocation(99),
      wikiLinks: [wikiLinkFromAuthoredToken("Grammar", 42)],
      lastSavedMarkdown: authoredMarkdown,
    })
    await expandPropertyPanel(unsavedWrapper, urlRowSelector)
    const unsavedRows = detailedRows(unsavedWrapper)
    expect(detailedLabels(unsavedWrapper)).toEqual(expectedSequence)
    expect(
      unsavedRows[1]!
        .find('[data-testid="rich-note-property-external-link"]')
        .exists()
    ).toBe(true)
    expect(
      unsavedWrapper.findAll(
        `${urlRowSelector} [data-testid="rich-note-property-panel"] [data-test="assimilate-UNDERSTANDING"]`
      )
    ).toHaveLength(0)
  })

  it("assimilates one value and keeps the other offered", async () => {
    const runTracker = makeMe.aMemoryTracker
      .id(1)
      .withPropertyKey("example of", "[[run]]")
      .please()
    const assimilateSpy = mockSdkService(AssimilationController, "assimilate", [
      runTracker,
    ])
    mockSdkService(AssimilationController, "next", {
      nextUnit: { noteId, propertyKey: "example of" },
      counts: { dueCount: 1 },
    })
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
