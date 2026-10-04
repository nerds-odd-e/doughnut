import { noteShowLocation } from "@/routes/noteShowLocation"
import { formatRelationshipNoteTitle } from "@/utils/relationshipNoteCompose"
import makeMe from "donut-test-fixtures/makeMe"
import { countHistoryEntriesAdded, productionRouterAt } from "@tests/helpers"
import { sidebarStructuralRefreshKey } from "@/components/notes/sidebarStructuralRefresh"
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

describe("AddRelationshipFinalize", () => {
  beforeEach(() => {
    vi.resetAllMocks()
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
    mockRelationshipNoteCreation(createdRealm, createHeld)

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
    const createNoteSpy = mockRelationshipNoteCreation(createdRealm)
    const router = await productionRouterAt(noteShowLocation(note.id))

    const navigating = mountAddRelationshipFinalize({
      note,
      targetSearchResult: target,
      seedRealm: sourceRealm,
      router,
    })
    const historyEntriesAdded = countHistoryEntriesAdded(router)
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
    expect(router.currentRoute.value).toMatchObject(
      noteShowLocation(createdRealm.id)
    )
    expect(historyEntriesAdded()).toBe(0)
    expect(navigating.emitted().success).toHaveLength(1)

    createNoteSpy.mockClear()
    const stayingRouter = await productionRouterAt(noteShowLocation(note.id))

    const withoutNav = mountAddRelationshipFinalize({
      note,
      targetSearchResult: target,
      seedRealm: sourceRealm,
      navigateOnSuccess: false,
      router: stayingRouter,
    })
    await selectRelationType(withoutNav, "related to")

    expect(stayingRouter.currentRoute.value).toMatchObject(
      noteShowLocation(note.id)
    )
    expect(withoutNav.emitted().success).toHaveLength(1)
  })

  describe("placing the relationship note in a child folder", () => {
    const sourceRealm = makeMe.aNoteRealm
      .title("Source")
      .inFolder(5, "Topics")
      .please()

    const createRelationshipNote = async (
      placement?: "named_after_source_note"
    ) => {
      const createNoteSpy = mockRelationshipNoteCreation(
        makeMe.aNoteRealm.please()
      )
      const wrapper = mountAddRelationshipFinalize({
        note: sourceRealm.note,
        targetSearchResult: targetSearchResult(),
        seedRealm: sourceRealm,
        navigateOnSuccess: false,
      })
      if (placement) {
        await wrapper
          .find(`#relationship-placement-${placement}`)
          .setValue(true)
      }
      await selectRelationType(wrapper, "related to")
      return createNoteSpy
    }

    it("lets the server place the note in the relations folder by default", async () => {
      const refreshKeyBefore = sidebarStructuralRefreshKey.value
      const createNoteSpy = await createRelationshipNote()
      expect(sidebarStructuralRefreshKey.value).toBe(refreshKeyBefore + 1)
      expect(createNoteSpy).toHaveBeenCalledTimes(1)
      expect(createNoteSpy.mock.calls[0]![0].body).toMatchObject({
        folderId: 5,
        childFolderName: "relations",
      })
    })

    it("lets the server place the note in a folder named after the source", async () => {
      const createNoteSpy = await createRelationshipNote(
        "named_after_source_note"
      )
      expect(createNoteSpy.mock.calls[0]![0].body).toMatchObject({
        folderId: 5,
        childFolderName: "Source",
      })
    })
  })
})
