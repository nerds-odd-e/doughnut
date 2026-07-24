import {
  NoteController,
  NotebookController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import MatchedNoteLinkOffer from "@/components/recall/MatchedNoteLinkOffer.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import helper, { mockSdkService } from "@tests/helpers"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import makeMe from "doughnut-test-fixtures/makeMe"
import type { NoteRealm } from "@generated/doughnut-backend-api"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"

function buildReviewedAndMatched(): {
  reviewedRealm: NoteRealm
  matchedRealm: NoteRealm
} {
  const reviewedRealm = makeMe.aNoteRealm
    .title("Reviewed Note")
    .content("Body of reviewed")
    .please()
  const matchedRealm = makeMe.aNoteRealm.title("Matched Target").please()
  matchedRealm.id = 20
  matchedRealm.note.id = 20
  matchedRealm.note.noteTopology.id = 20
  matchedRealm.note.noteTopology.title = "Matched Target"
  matchedRealm.notebookRealm.notebook.id =
    reviewedRealm.notebookRealm.notebook.id
  matchedRealm.notebookRealm.notebook.name =
    reviewedRealm.notebookRealm.notebook.name
  return { reviewedRealm, matchedRealm }
}

function mountOffer(reviewedRealm: NoteRealm, matchedRealm: NoteRealm) {
  const chain = helper.component(MatchedNoteLinkOffer).withCleanStorage()
  useStorageAccessor().value.refreshNoteRealm(reviewedRealm)
  useStorageAccessor().value.refreshNoteRealm(matchedRealm)
  return chain
    .withProps({
      reviewedNoteId: reviewedRealm.id,
      matchedNoteId: matchedRealm.id,
    })
    .mount()
}

describe("MatchedNoteLinkOffer", () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
  })

  afterEach(() => {
    teardownGlobalClientForTesting()
  })

  it("shows Link to: with matched title, hides bare wiki and relationship options", async () => {
    const { reviewedRealm, matchedRealm } = buildReviewedAndMatched()
    const updateSpy = mockSdkService(
      TextContentController,
      "updateNoteContent",
      reviewedRealm
    )
    const createSpy = mockSdkService(
      NotebookController,
      "createNoteAtNotebookRoot",
      matchedRealm
    )

    const wrapper = mountOffer(reviewedRealm, matchedRealm)
    await flushPromises()

    expect(wrapper.text()).toContain("Link to:")
    expect(wrapper.text()).toContain("Matched Target")
    expect(wrapper.text()).not.toContain("Insert as a wiki link")
    expect(wrapper.text()).toContain("Add wiki link as a new property")
    expect(wrapper.text()).not.toContain("Add a new relationship note")
    expect(wrapper.find("input").exists()).toBe(false)
    expect(updateSpy).not.toHaveBeenCalled()
    expect(createSpy).not.toHaveBeenCalled()
  })

  it("writes a wiki-link property via updateNoteContent and emits closeDialog", async () => {
    const { reviewedRealm, matchedRealm } = buildReviewedAndMatched()
    const updateSpy = mockSdkService(
      TextContentController,
      "updateNoteContent",
      reviewedRealm
    )
    const createSpy = mockSdkService(
      NotebookController,
      "createNoteAtNotebookRoot",
      matchedRealm
    )

    const wrapper = mountOffer(reviewedRealm, matchedRealm)
    await flushPromises()

    await wrapper
      .findAll("button")
      .find((b) => b.text().includes("Add wiki link as a new property"))!
      .trigger("click")
    await flushPromises()

    expect(updateSpy).toHaveBeenCalledTimes(1)
    const callArgs = updateSpy.mock.calls[0]![0] as {
      path: { note: number }
      body: { content?: string }
    }
    expect(callArgs.path.note).toBe(reviewedRealm.id)
    expect(callArgs.body.content).toContain("[[Matched Target]]")
    expect(createSpy).not.toHaveBeenCalled()
    expect(wrapper.emitted("closeDialog")).toHaveLength(1)
  })

  it("does not call content or create APIs on mount alone", async () => {
    const { reviewedRealm, matchedRealm } = buildReviewedAndMatched()
    const updateSpy = mockSdkService(
      TextContentController,
      "updateNoteContent",
      reviewedRealm
    )
    const createSpy = mockSdkService(
      NotebookController,
      "createNoteAtNotebookRoot",
      matchedRealm
    )

    mountOffer(reviewedRealm, matchedRealm)
    await flushPromises()

    expect(updateSpy).not.toHaveBeenCalled()
    expect(createSpy).not.toHaveBeenCalled()
  })
})
