import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { describe, expect, it, vi } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { mockedGoToNextAssimilation } from "./assimilationPanelMocks"
import {
  assimilateButtonEl,
  assimilateSpy,
  assimilatedCountOfTheDay,
  clickRememberSpelling,
  clickVerifySpelling,
  closeSpellingVerificationPopup,
  mockedRequestDueRecallsRefresh,
  mockedTotalAssimilatedCount,
  mountAssimilationPanelReady,
  note,
  opaqueContentBlockerEl,
  rememberSpellingButtonEl,
  setupAssimilationPanelTests,
  spellingStatusEl,
  spellingVerificationPopupEl,
} from "./assimilationPanelTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useGoToNextAssimilation", () => ({
  useGoToNextAssimilation: () => ({
    goToNextAssimilation: mockedGoToNextAssimilation,
  }),
}))

setupAssimilationPanelTests()

describe("AssimilationPanel remember spelling", () => {
  it("shows spelling verification without posting when remember spelling is chosen", async () => {
    const wrapper = await mountAssimilationPanelReady()

    await clickRememberSpelling(wrapper)

    const opaqueLayer = opaqueContentBlockerEl()
    expect(opaqueLayer).not.toBeNull()
    expect(opaqueLayer?.style.zIndex).toBe("9989")
    expect(opaqueLayer?.className).toContain("bg-black")
    expect(spellingVerificationPopupEl()).not.toBeNull()
    expect(assimilateSpy).not.toHaveBeenCalled()
  })

  it("posts assimilateAsSpelling after verified spelling and stays on note", async () => {
    mockSdkService(NoteController, "verifySpelling", { correct: true })
    assimilateSpy.mockResolvedValue(
      wrapSdkResponse([makeMe.aMemoryTracker.id(1).spelling().please()])
    )
    const wrapper = await mountAssimilationPanelReady()

    await clickRememberSpelling(wrapper)
    await clickVerifySpelling()

    expect(assimilateSpy).toHaveBeenCalledWith({
      body: { noteId: note.id, assimilateAsSpelling: true },
    })
    expect(mockedGoToNextAssimilation).not.toHaveBeenCalled()
    expect(mockedTotalAssimilatedCount.value).toBe(0)
    expect(assimilatedCountOfTheDay.value).toBe(0)
    expect(mockedRequestDueRecallsRefresh).toHaveBeenCalled()
  })

  it("does not post when spelling verification is cancelled", async () => {
    const wrapper = await mountAssimilationPanelReady()

    await clickRememberSpelling(wrapper)
    await closeSpellingVerificationPopup()

    expect(assimilateSpy).not.toHaveBeenCalled()
    expect(spellingVerificationPopupEl()).toBeNull()
  })

  it("enables assimilate when note has only a spelling memory tracker", async () => {
    mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [makeMe.aMemoryTracker.id(1).spelling().please()],
    })
    const wrapper = await mountAssimilationPanelReady()

    expect(assimilateButtonEl(wrapper)?.hasAttribute("disabled")).toBe(false)
  })

  it("shows the tracker status instead of remember spelling when a spelling tracker already exists", async () => {
    mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [makeMe.aMemoryTracker.id(1).spelling().please()],
    })
    const wrapper = await mountAssimilationPanelReady()

    expect(rememberSpellingButtonEl(wrapper)).toBeNull()
    expect(spellingStatusEl(wrapper)).not.toBeNull()
  })

  it("offers remember spelling when a commissioned tracker already exists", async () => {
    mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [makeMe.aMemoryTracker.id(1).commissioned().please()],
    })
    const wrapper = await mountAssimilationPanelReady()

    expect(rememberSpellingButtonEl(wrapper)).not.toBeNull()
  })

  it("offers remember spelling when ordinary trackers already exist", async () => {
    mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [makeMe.aMemoryTracker.id(1).spelling(false).please()],
    })
    const wrapper = await mountAssimilationPanelReady()

    expect(assimilateButtonEl(wrapper)).toBeNull()
    expect(rememberSpellingButtonEl(wrapper)).not.toBeNull()
  })
})
