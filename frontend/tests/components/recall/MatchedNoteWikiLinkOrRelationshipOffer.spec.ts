import {
  NoteController,
  NotebookController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import MatchedNoteWikiLinkOrRelationshipOffer from "@/components/recall/MatchedNoteWikiLinkOrRelationshipOffer.vue"
import RelationTypeSelect from "@/components/wiki-link-or-relationship/RelationTypeSelect.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import helper, { mockSdkService, testFolderStub } from "@tests/helpers"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import makeMe from "donut-test-fixtures/makeMe"
import type { NoteRealm } from "@generated/donut-backend-api"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"

const routerReplace = vi.fn()

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      replace: routerReplace,
    }),
  }
})

function buildReviewedAndMatched(): {
  reviewedRealm: NoteRealm
  matchedRealm: NoteRealm
} {
  const reviewedRealm = makeMe.aNoteRealm
    .title("Reviewed Note")
    .content("Body of reviewed")
    .please()
  const matchedRealm = makeMe.aNoteRealm
    .id(20)
    .title("Matched Target")
    .inNotebook(
      reviewedRealm.notebookRealm.notebook.id,
      reviewedRealm.notebookRealm.notebook.name
    )
    .please()
  return { reviewedRealm, matchedRealm }
}

function mountOffer(reviewedRealm: NoteRealm, matchedRealm: NoteRealm) {
  const chain = helper
    .component(MatchedNoteWikiLinkOrRelationshipOffer)
    .withCleanStorage()
  useStorageAccessor().value.refreshNoteRealm(reviewedRealm)
  useStorageAccessor().value.refreshNoteRealm(matchedRealm)
  return chain
    .withProps({
      reviewedNoteId: reviewedRealm.id,
      matchedNoteId: matchedRealm.id,
    })
    .mount()
}

async function selectRelationType(wrapper: VueWrapper, relationType: string) {
  await wrapper
    .findComponent(RelationTypeSelect)
    .vm.$emit("update:modelValue", relationType)
  await flushPromises()
}

async function clickInsertWikiLinkAsProperty(wrapper: VueWrapper) {
  await wrapper
    .findAll("button")
    .find((b) => b.text().includes("Add wiki link as a new property"))!
    .trigger("click")
  await flushPromises()
}

function mockRelationshipCreate(
  sourceRealm: NoteRealm,
  createdRealm: NoteRealm
) {
  mockSdkService(NoteController, "showNote", sourceRealm)
  mockSdkService(TextContentController, "updateNoteContent", sourceRealm)
  mockSdkService(NotebookController, "listNotebookFolderListing", {
    folders: [],
  })
  mockSdkService(
    NotebookController,
    "createFolder",
    testFolderStub(77, "relations")
  )
  return mockSdkService(
    NotebookController,
    "createNoteAtNotebookRoot",
    createdRealm
  )
}

describe("MatchedNoteWikiLinkOrRelationshipOffer", () => {
  beforeEach(() => {
    vi.resetAllMocks()
    routerReplace.mockResolvedValue(undefined)
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
  })

  afterEach(() => {
    teardownGlobalClientForTesting()
  })

  it("shows Target: with matched title, property and relationship options, hides bare wiki", async () => {
    const { reviewedRealm, matchedRealm } = buildReviewedAndMatched()

    const wrapper = mountOffer(reviewedRealm, matchedRealm)
    await flushPromises()

    expect(wrapper.text()).toContain("Target:")
    expect(wrapper.text()).toContain("Matched Target")
    expect(wrapper.text()).not.toContain("Insert as a wiki link")
    expect(wrapper.text()).toContain("Add wiki link as a new property")
    expect(wrapper.text()).toContain("Add a new relationship note")
    expect(wrapper.find("input").exists()).toBe(false)
  })

  it("writes the backend-authored Portable path as a wiki-link property and closes", async () => {
    const { reviewedRealm, matchedRealm } = buildReviewedAndMatched()
    const authoredPortablePathSpy = mockSdkService(
      NoteController,
      "authoredPortablePath",
      { portablePath: "Folder/Matched Target" }
    )
    const updateSpy = mockSdkService(
      TextContentController,
      "updateNoteContent",
      reviewedRealm
    )

    const wrapper = mountOffer(reviewedRealm, matchedRealm)
    await flushPromises()

    await clickInsertWikiLinkAsProperty(wrapper)

    expect(authoredPortablePathSpy).toHaveBeenCalledTimes(1)
    const callArgs = updateSpy.mock.calls[0]![0] as {
      path: { note: number }
      body: { content?: string }
    }
    expect(callArgs.path.note).toBe(reviewedRealm.id)
    expect(callArgs.body.content).toContain("[[Folder/Matched Target]]")
    expect(wrapper.emitted("closeDialog")).toHaveLength(1)
  })

  it("does not create a relationship note until the user confirms relation type", async () => {
    const { reviewedRealm, matchedRealm } = buildReviewedAndMatched()
    const createdRealm = makeMe.aNoteRealm
      .title("Created relationship")
      .please()
    const createSpy = mockRelationshipCreate(reviewedRealm, createdRealm)

    const wrapper = mountOffer(reviewedRealm, matchedRealm)
    await flushPromises()

    await wrapper
      .findAll("button")
      .find((b) => b.text().includes("Add a new relationship note"))!
      .trigger("click")
    await flushPromises()

    expect(createSpy).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain("Relation note location")
  })

  it("creates relationship note, closes dialog, and does not navigate", async () => {
    const { reviewedRealm, matchedRealm } = buildReviewedAndMatched()
    const createdRealm = makeMe.aNoteRealm
      .title("Created relationship")
      .please()
    const createSpy = mockRelationshipCreate(reviewedRealm, createdRealm)

    const wrapper = mountOffer(reviewedRealm, matchedRealm)
    await flushPromises()

    await wrapper
      .findAll("button")
      .find((b) => b.text().includes("Add a new relationship note"))!
      .trigger("click")
    await flushPromises()

    await selectRelationType(wrapper, "related to")

    expect(createSpy).toHaveBeenCalledTimes(1)
    expect(routerReplace).not.toHaveBeenCalled()
    expect(wrapper.emitted("closeDialog")).toHaveLength(1)
  })
})
