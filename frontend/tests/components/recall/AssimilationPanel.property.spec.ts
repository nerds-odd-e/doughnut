import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi, beforeEach } from "vitest"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { mockedGoToNextAssimilation } from "./assimilationPanelMocks"
import {
  clickPropertyKeepForRecall,
  expandAssimilationPropertiesSection,
  noteWithAssimilationProperties,
} from "./assimilationPropertyTestSupport"
import {
  assimilateSpy,
  mockedIncrementAssimilatedCount,
  mockedRequestDueRecallsRefresh,
  renderer,
  setupAssimilationPanelTests,
} from "./assimilationPanelTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useAssimilationCount")
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

  it("advances to the next unit and reloads note info when keeping a property for recall", async () => {
    assimilateSpy.mockResolvedValue(
      wrapSdkResponse([{ id: 1, removedFromTracking: false }])
    )
    const wrapper = mountPanelWithProperties()
    await flushPromises()
    await expandAssimilationPropertiesSection()
    await clickPropertyKeepForRecall("topic")

    expect(assimilateSpy).toHaveBeenCalledWith({
      body: {
        noteId: noteWithAssimilationProperties.id,
        propertyKey: "topic",
      },
    })
    expect(mockedGoToNextAssimilation).toHaveBeenCalled()
    expect(mockedIncrementAssimilatedCount).toHaveBeenCalledWith(1)
    expect(mockedRequestDueRecallsRefresh).toHaveBeenCalled()
    expect(getNoteInfoSpy.mock.calls.length).toBeGreaterThanOrEqual(2)
    wrapper.unmount()
  })
})
