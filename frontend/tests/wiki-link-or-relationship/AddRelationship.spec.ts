import { NotebookController } from "@generated/donut-backend-api/sdk.gen"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { formatRelationshipNoteTitle } from "@/utils/relationshipNoteCompose"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, testFolderStub } from "@tests/helpers"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import { nextTick } from "vue"
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"
import {
  mountAddRelationshipFinalize,
  mockRelationshipNoteCreation,
  selectRelationType,
  sourceAndCreatedRelationshipRealms,
  targetSearchResult,
} from "./addRelationshipFinalizeTestSupport"

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

describe("AddRelationshipFinalize", () => {
  beforeEach(() => {
    vi.resetAllMocks()
    routerReplace.mockResolvedValue(undefined)
    mockSdkService(NotebookController, "listNotebookFolderListing", {
      folders: [],
    })
    mockSdkService(
      NotebookController,
      "createFolder",
      testFolderStub(77, "relations")
    )
  })

  afterEach(() => {
    teardownGlobalClientForTesting()
  })

  it("shows placement options with relations subfolder selected by default", () => {
    const note = makeMe.aNote.please()
    const wrapper = mountAddRelationshipFinalize({
      note,
      targetSearchResult: targetSearchResult(),
    })

    const defaultRadio = wrapper.find(
      "#relationship-placement-relations_subfolder"
    )
    expect((defaultRadio.element as HTMLInputElement).checked).toBe(true)
  })

  it("emits goBack when back button is clicked", async () => {
    const note = makeMe.aNote.please()
    const wrapper = mountAddRelationshipFinalize({
      note,
      targetSearchResult: targetSearchResult(),
    })

    await wrapper.find(".go-back-button").trigger("click")
    expect(wrapper.emitted().goBack).toHaveLength(1)
  })

  it("shows LoadingModal while creating relationship note", async () => {
    const { sourceRealm, note, createdRealm } =
      sourceAndCreatedRelationshipRealms()
    let resolveCreate: () => void
    const createHeld = new Promise<void>((r) => {
      resolveCreate = r
    })
    mockRelationshipNoteCreation(sourceRealm, createdRealm, createHeld)

    const wrapper = mountAddRelationshipFinalize({
      note,
      targetSearchResult: targetSearchResult(),
      seedRealm: sourceRealm,
      withLoadingModal: true,
    })

    const selectPromise = selectRelationType(wrapper, "related to")
    await nextTick()

    expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
    expect(document.body.textContent).toContain("Creating relationship note...")

    resolveCreate!()
    await selectPromise

    expect(document.querySelector(".loading-modal-mask")).toBeNull()
  })

  it("creates relationship note, navigates when enabled, and skips navigate when disabled", async () => {
    const { sourceRealm, note, createdRealm } =
      sourceAndCreatedRelationshipRealms()
    const target = targetSearchResult()
    const createNoteSpy = mockRelationshipNoteCreation(
      sourceRealm,
      createdRealm
    )

    const navigating = mountAddRelationshipFinalize({
      note,
      targetSearchResult: target,
      seedRealm: sourceRealm,
    })
    await selectRelationType(navigating, "related to")

    const expectedTitle = formatRelationshipNoteTitle(
      note.noteTopology.title,
      "related to",
      target.noteTopology.title
    )
    expect(createNoteSpy).toHaveBeenCalledWith({
      path: { notebook: sourceRealm.notebookRealm.notebook.id },
      body: expect.objectContaining({
        newTitle: expectedTitle,
        content: expect.stringContaining("type: Relationship"),
      }),
    })
    expect(routerReplace).toHaveBeenCalledWith(
      noteShowLocation(createdRealm.id)
    )
    expect(navigating.emitted().success).toHaveLength(1)

    routerReplace.mockClear()
    createNoteSpy.mockClear()

    const withoutNav = mountAddRelationshipFinalize({
      note,
      targetSearchResult: target,
      seedRealm: sourceRealm,
      navigateOnSuccess: false,
    })
    await selectRelationType(withoutNav, "related to")

    expect(routerReplace).not.toHaveBeenCalled()
    expect(withoutNav.emitted().success).toHaveLength(1)
  })
})
