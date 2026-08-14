import {
  AssimilationSequenceSkipController,
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
  propertyReturnToSequenceButton,
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
})
