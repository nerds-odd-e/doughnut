import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { describe, expect, it, vi } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { mockedGoToNextAssimilation } from "./assimilationPanelMocks"
import {
  assimilateAsCommissionedButtonSelector,
  assimilateButtonSelector,
  assimilateSpy,
  assimilatedCountOfTheDay,
  clickAssimilateAsCommissioned,
  clickRememberSpelling,
  clickVerifySpelling,
  closeSpellingVerificationPopup,
  commissionedStatusSelector,
  mockedRequestDueRecallsRefresh,
  mockedTotalAssimilatedCount,
  mountAssimilationPanelReady,
  note,
  opaqueContentBlockerEl,
  rememberSpellingButtonSelector,
  setupAssimilationPanelTests,
  spellingStatusSelector,
  spellingVerificationPopupEl,
  understandingStatusSelector,
} from "./assimilationPanelTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useGoToNextAssimilation", () => ({
  useGoToNextAssimilation: () => ({
    goToNextAssimilation: mockedGoToNextAssimilation,
  }),
}))

setupAssimilationPanelTests()

const tracker = () => makeMe.aMemoryTracker.id(1)
const understanding = tracker().spelling(false).please()

function expectStaysOnNoteAfterAssimilateAs(body: object) {
  expect(assimilateSpy).toHaveBeenCalledWith({
    body: { noteId: note.id, ...body },
  })
  expect(mockedGoToNextAssimilation).not.toHaveBeenCalled()
  expect(mockedTotalAssimilatedCount.value).toBe(0)
  expect(assimilatedCountOfTheDay.value).toBe(0)
  expect(mockedRequestDueRecallsRefresh).toHaveBeenCalled()
}

describe("AssimilationPanel actions offered for existing memory trackers", () => {
  it.each([
    {
      existing: "only a property tracker",
      trackers: [tracker().withPropertyKey("topic").spelling(false).please()],
      offered: [assimilateButtonSelector],
      hidden: [],
    },
    {
      existing: "only a commissioned tracker",
      trackers: [tracker().commissioned().please()],
      offered: [assimilateButtonSelector, rememberSpellingButtonSelector],
      hidden: [],
    },
    {
      existing: "only a spelling tracker",
      trackers: [tracker().spelling().please()],
      offered: [assimilateButtonSelector, spellingStatusSelector],
      hidden: [rememberSpellingButtonSelector],
    },
    {
      existing: "an understanding tracker",
      trackers: [understanding],
      offered: [
        understandingStatusSelector,
        rememberSpellingButtonSelector,
        assimilateAsCommissionedButtonSelector,
      ],
      hidden: [assimilateButtonSelector],
    },
    {
      existing: "understanding and commissioned trackers",
      trackers: [
        understanding,
        makeMe.aMemoryTracker.id(2).commissioned().please(),
      ],
      offered: [commissionedStatusSelector],
      hidden: [assimilateAsCommissionedButtonSelector],
    },
  ])("with $existing", async ({ trackers, offered, hidden }) => {
    mockSdkService(NoteController, "getNoteInfo", { memoryTrackers: trackers })
    const wrapper = await mountAssimilationPanelReady()

    for (const selector of offered) {
      const el = wrapper.element.querySelector(selector)
      expect(el, selector).not.toBeNull()
      expect(el?.hasAttribute("disabled"), selector).toBe(false)
    }
    for (const selector of hidden) {
      expect(wrapper.element.querySelector(selector), selector).toBeNull()
    }
  })
})

describe("AssimilationPanel commissioned assimilation", () => {
  it("posts assimilateAsCommissioned and stays on note without navigating", async () => {
    assimilateSpy.mockResolvedValue(
      wrapSdkResponse([tracker().commissioned().please()])
    )
    const wrapper = await mountAssimilationPanelReady()

    await clickAssimilateAsCommissioned(wrapper)

    expectStaysOnNoteAfterAssimilateAs({ assimilateAsCommissioned: true })
    expect(spellingVerificationPopupEl()).toBeNull()
  })
})

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
      wrapSdkResponse([tracker().spelling().please()])
    )
    const wrapper = await mountAssimilationPanelReady()

    await clickRememberSpelling(wrapper)
    await clickVerifySpelling()

    expectStaysOnNoteAfterAssimilateAs({ assimilateAsSpelling: true })
  })

  it("does not post when spelling verification is cancelled", async () => {
    const wrapper = await mountAssimilationPanelReady()

    await clickRememberSpelling(wrapper)
    await closeSpellingVerificationPopup()

    expect(assimilateSpy).not.toHaveBeenCalled()
    expect(spellingVerificationPopupEl()).toBeNull()
  })
})
