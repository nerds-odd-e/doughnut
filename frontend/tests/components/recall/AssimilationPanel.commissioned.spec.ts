import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { mockedGoToNextAssimilation } from "./assimilationPanelMocks"
import {
  assimilateAsCommissionedButtonEl,
  assimilateButtonEl,
  assimilateOptionsCaretEl,
  assimilateSpy,
  assimilatedCountOfTheDay,
  clickAssimilateAsCommissioned,
  mockedRequestDueRecallsRefresh,
  mockedTotalAssimilatedCount,
  mountAssimilationPanelReady,
  note,
  openAssimilateOptions,
  setupAssimilationPanelTests,
  spellingVerificationPopupEl,
} from "./assimilationPanelTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useGoToNextAssimilation", () => ({
  useGoToNextAssimilation: () => ({
    goToNextAssimilation: mockedGoToNextAssimilation,
  }),
}))

setupAssimilationPanelTests()

describe("AssimilationPanel commissioned assimilation", () => {
  it("posts assimilateAsCommissioned and stays on note without navigating", async () => {
    assimilateSpy.mockResolvedValue(
      wrapSdkResponse([makeMe.aMemoryTracker.id(1).commissioned().please()])
    )
    const wrapper = await mountAssimilationPanelReady()

    await clickAssimilateAsCommissioned(wrapper)

    expect(assimilateSpy).toHaveBeenCalledWith({
      body: { noteId: note.id, assimilateAsCommissioned: true },
    })
    expect(mockedGoToNextAssimilation).not.toHaveBeenCalled()
    expect(mockedTotalAssimilatedCount.value).toBe(0)
    expect(assimilatedCountOfTheDay.value).toBe(0)
    expect(mockedRequestDueRecallsRefresh).toHaveBeenCalled()
    expect(spellingVerificationPopupEl()).toBeNull()
  })

  it("enables assimilate when note has only a commissioned memory tracker", async () => {
    mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [makeMe.aMemoryTracker.id(1).commissioned().please()],
    })
    const wrapper = await mountAssimilationPanelReady()

    expect(assimilateButtonEl(wrapper)?.hasAttribute("disabled")).toBe(false)
  })

  it("hides commissioned option when note already has a commissioned tracker", async () => {
    mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [
        makeMe.aMemoryTracker.id(1).spelling(false).please(),
        makeMe.aMemoryTracker.id(2).commissioned().please(),
      ],
    })
    const wrapper = await mountAssimilationPanelReady()

    expect(assimilateOptionsCaretEl(wrapper)).not.toBeNull()
    await openAssimilateOptions(wrapper)
    expect(assimilateAsCommissionedButtonEl()).toBeNull()
  })

  it("keeps commissioned option usable when ordinary trackers already exist", async () => {
    mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [makeMe.aMemoryTracker.id(1).spelling(false).please()],
    })
    assimilateSpy.mockResolvedValue(
      wrapSdkResponse([makeMe.aMemoryTracker.id(2).commissioned().please()])
    )
    const wrapper = await mountAssimilationPanelReady()

    expect(assimilateButtonEl(wrapper)?.hasAttribute("disabled")).toBe(true)
    expect(assimilateOptionsCaretEl(wrapper)).not.toBeNull()
    await clickAssimilateAsCommissioned(wrapper)
    expect(assimilateSpy).toHaveBeenCalled()
  })
})
