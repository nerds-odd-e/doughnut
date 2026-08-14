import {
  AssimilationSequenceSkipController,
  MemoryTrackerController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi, beforeEach } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkResponse,
} from "@tests/helpers"
import { mockedGoToNextAssimilation } from "./assimilationPanelMocks"
import {
  clickPropertyAssimilate,
  clickPropertyRemoveFromRecallAndConfirm,
  clickPropertySkipAndConfirm,
  expandAssimilationPropertiesSection,
  noteWithAssimilationProperties,
} from "./assimilationPropertyTestSupport"
import {
  assimilateSpy,
  assimilatedCountOfTheDay,
  mockedRequestDueRecallsRefresh,
  renderer,
  setupAssimilationPanelTests,
  skipSequenceSpy,
} from "./assimilationPanelTestSupport"
import {
  clickPropertyReturnToSequence,
  propertyRemoveFromRecallButton,
  propertyReturnToSequenceButton,
  propertyReviveButton,
  propertySkipButton,
} from "./assimilationSettingsTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useGoToNextAssimilation", () => ({
  useGoToNextAssimilation: () => ({
    goToNextAssimilation: mockedGoToNextAssimilation,
  }),
}))

setupAssimilationPanelTests()

describe("AssimilationPanel property assimilation", () => {
  let getNoteInfoSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    getNoteInfoSpy = mockSdkService(NoteController, "getNoteInfo", {})
  })

  const mountPanelWithProperties = () =>
    renderer
      .withCleanStorage()
      .withProps({ note: noteWithAssimilationProperties })
      .withRouter()
      .mount({ attachTo: document.body })

  it("advances to the next unit and reloads note info when assimilating a property", async () => {
    assimilateSpy.mockResolvedValue(
      wrapSdkResponse([makeMe.aMemoryTracker.id(1).please()])
    )
    const wrapper = mountPanelWithProperties()
    await flushPromises()
    await expandAssimilationPropertiesSection()
    await clickPropertyAssimilate("topic")

    expect(assimilateSpy).toHaveBeenCalledWith({
      body: {
        noteId: noteWithAssimilationProperties.id,
        propertyKey: "topic",
      },
    })
    expect(mockedGoToNextAssimilation).toHaveBeenCalled()
    expect(assimilatedCountOfTheDay.value).toBe(1)
    expect(mockedRequestDueRecallsRefresh).toHaveBeenCalled()
    expect(getNoteInfoSpy.mock.calls.length).toBeGreaterThanOrEqual(2)
    wrapper.unmount()
  })

  it("skips a property from the sequence without creating a tracker", async () => {
    const wrapper = mountPanelWithProperties()
    await flushPromises()
    await expandAssimilationPropertiesSection()
    await clickPropertySkipAndConfirm("topic")

    expect(skipSequenceSpy).toHaveBeenCalledWith({
      body: {
        noteId: noteWithAssimilationProperties.id,
        propertyKey: "topic",
      },
    })
    expect(assimilateSpy).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it("returns a skipped property to the sequence without creating a tracker or reviving", async () => {
    let skippedKeys = ["topic"]
    mockSdkServiceWithImplementation(NoteController, "getNoteInfo", () =>
      makeMe.aNoteRecallInfo.skippedPropertyKeys(skippedKeys).please()
    )
    const deleteSkipSpy = mockSdkService(
      AssimilationSequenceSkipController,
      "deleteAssimilationSequenceSkip",
      undefined as never
    )
    deleteSkipSpy.mockImplementation(async () => {
      skippedKeys = []
      return wrapSdkResponse(undefined)
    })
    const wrapper = mountPanelWithProperties()
    await flushPromises()
    await expandAssimilationPropertiesSection()

    await clickPropertyReturnToSequence("topic")

    expect(deleteSkipSpy).toHaveBeenCalledWith({
      body: {
        noteId: noteWithAssimilationProperties.id,
        propertyKey: "topic",
      },
    })
    expect(assimilateSpy).not.toHaveBeenCalled()
    expect(propertySkipButton("topic")).not.toBeNull()
    expect(propertyReturnToSequenceButton("topic")).toBeNull()
    wrapper.unmount()
  })

  it("removes a property tracker from recall and shows Revive", async () => {
    let removedFromTracking = false
    mockSdkServiceWithImplementation(NoteController, "getNoteInfo", () =>
      makeMe.aNoteRecallInfo
        .memoryTrackers([
          makeMe.aMemoryTracker
            .id(7)
            .withPropertyKey("topic")
            .removedFromTracking(removedFromTracking)
            .please(),
        ])
        .please()
    )
    const removeSpy = mockSdkService(
      MemoryTrackerController,
      "removeFromRepeating",
      makeMe.aMemoryTracker
        .id(7)
        .withPropertyKey("topic")
        .removedFromTracking(true)
        .please()
    )
    removeSpy.mockImplementation(async () => {
      removedFromTracking = true
      return wrapSdkResponse(
        makeMe.aMemoryTracker
          .id(7)
          .withPropertyKey("topic")
          .removedFromTracking(true)
          .please()
      )
    })
    const wrapper = mountPanelWithProperties()
    await flushPromises()
    await expandAssimilationPropertiesSection()

    await clickPropertyRemoveFromRecallAndConfirm("topic")

    expect(removeSpy).toHaveBeenCalledWith({
      path: { memoryTracker: 7 },
    })
    expect(skipSequenceSpy).not.toHaveBeenCalled()
    expect(mockedGoToNextAssimilation).not.toHaveBeenCalled()
    expect(propertyReviveButton("topic")).not.toBeNull()
    expect(propertySkipButton("topic")).toBeNull()
    expect(propertyRemoveFromRecallButton("topic")).toBeNull()
    wrapper.unmount()
  })
})
