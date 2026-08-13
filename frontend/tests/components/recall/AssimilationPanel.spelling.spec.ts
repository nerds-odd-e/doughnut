import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { mockedGoToNextAssimilation } from "./assimilationPanelMocks"
import {
  assimilateButtonEl,
  assimilateOptionsCaretEl,
  assimilateSpy,
  assimilatedCountOfTheDay,
  clickRememberSpelling,
  clickVerifySpelling,
  closeSpellingVerificationPopup,
  mockedRequestDueRecallsRefresh,
  mockedTotalAssimilatedCount,
  mountAssimilationPanelReady,
  note,
  openAssimilateOptions,
  rememberSpellingButtonEl,
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

describe("AssimilationPanel remember spelling", () => {
  it("shows spelling verification without posting when remember spelling is chosen", async () => {
    const wrapper = await mountAssimilationPanelReady()

    await clickRememberSpelling(wrapper)

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

  it("hides remember spelling when a spelling tracker already exists", async () => {
    mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [makeMe.aMemoryTracker.id(1).spelling().please()],
    })
    const wrapper = await mountAssimilationPanelReady()

    expect(assimilateOptionsCaretEl(wrapper)).not.toBeNull()
    await openAssimilateOptions(wrapper)
    expect(rememberSpellingButtonEl()).toBeNull()
  })

  it("offers remember spelling when a commissioned tracker already exists", async () => {
    mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [makeMe.aMemoryTracker.id(1).commissioned().please()],
    })
    const wrapper = await mountAssimilationPanelReady()

    expect(assimilateOptionsCaretEl(wrapper)).not.toBeNull()
    await openAssimilateOptions(wrapper)
    expect(rememberSpellingButtonEl()).not.toBeNull()
  })

  it("offers remember spelling when ordinary trackers already exist", async () => {
    mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [makeMe.aMemoryTracker.id(1).spelling(false).please()],
    })
    const wrapper = await mountAssimilationPanelReady()

    expect(assimilateButtonEl(wrapper)?.hasAttribute("disabled")).toBe(true)
    expect(assimilateOptionsCaretEl(wrapper)).not.toBeNull()
    await openAssimilateOptions(wrapper)
    expect(rememberSpellingButtonEl()).not.toBeNull()
  })

  it("offers remember spelling on the note-level menu when the note has properties", async () => {
    const noteWithProperty = makeMe.aNote
      .id(note.id)
      .content("---\ntopic: Spanish\n---\n")
      .please()
    mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [],
    })
    const wrapper = await mountAssimilationPanelReady({
      note: noteWithProperty,
    })

    expect(assimilateOptionsCaretEl(wrapper)).not.toBeNull()
    await openAssimilateOptions(wrapper)
    expect(rememberSpellingButtonEl()).not.toBeNull()
  })

  it("hides remember spelling when the note has no content", async () => {
    const noteWithoutContent = makeMe.aNote.id(note.id).content("").please()
    const wrapper = await mountAssimilationPanelReady({
      note: noteWithoutContent,
    })

    expect(assimilateOptionsCaretEl(wrapper)).not.toBeNull()
    await openAssimilateOptions(wrapper)
    expect(rememberSpellingButtonEl()).toBeNull()
  })

  it("hides remember spelling for a relationship note", async () => {
    const relationshipNote = makeMe.aNote
      .id(note.id)
      .relationType("similar to")
      .please()
    const wrapper = await mountAssimilationPanelReady({
      note: relationshipNote,
    })

    expect(assimilateOptionsCaretEl(wrapper)).not.toBeNull()
    await openAssimilateOptions(wrapper)
    expect(rememberSpellingButtonEl()).toBeNull()
  })
})
