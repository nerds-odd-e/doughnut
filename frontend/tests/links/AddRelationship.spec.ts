import {
  NoteController,
  NotebookController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import AddRelationshipFinalize from "@/components/links/AddRelationshipFinalize.vue"
import RelationTypeSelect from "@/components/links/RelationTypeSelect.vue"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { formatRelationshipNoteTitle } from "@/utils/relationshipNoteCompose"
import makeMe from "doughnut-test-fixtures/makeMe"
import type {
  Note,
  NoteRealm,
  NoteSearchResult,
} from "@generated/doughnut-backend-api"
import helper, { mockSdkService, testFolderStub } from "@tests/helpers"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { describe, it, expect, beforeEach, vi } from "vitest"

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

function targetSearchResult(title = "Target"): NoteSearchResult {
  return makeMe.aNoteSearchResult.title(title).notebookId(1).do()
}

function mountAddRelationshipFinalize({
  note,
  targetSearchResult,
  seedRealm,
}: {
  note: Note
  targetSearchResult: NoteSearchResult
  seedRealm?: NoteRealm
}) {
  helper.component(AddRelationshipFinalize).withCleanStorage()
  if (seedRealm) {
    useStorageAccessor().value.refreshNoteRealm(seedRealm)
  }
  return helper
    .component(AddRelationshipFinalize)
    .withProps({ note, targetSearchResult })
    .mount()
}

async function selectRelationType(wrapper: VueWrapper, relationType: string) {
  await wrapper
    .findComponent(RelationTypeSelect)
    .vm.$emit("update:modelValue", relationType)
  await flushPromises()
}

function mockRelationshipNoteCreation(
  sourceRealm: NoteRealm,
  createdRealm: NoteRealm
) {
  mockSdkService(NoteController, "showNote", sourceRealm)
  mockSdkService(TextContentController, "updateNoteContent", sourceRealm)
  return mockSdkService(
    NotebookController,
    "createNoteAtNotebookRoot",
    createdRealm
  )
}

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

  it("shows placement options with relations subfolder selected by default", () => {
    const note = makeMe.aNote.please()
    const wrapper = mountAddRelationshipFinalize({
      note,
      targetSearchResult: targetSearchResult(),
    })

    expect(wrapper.find('[role="radiogroup"]').exists()).toBe(true)
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

  it("creates relationship note, navigates, and emits success", async () => {
    const sourceRealm = makeMe.aNoteRealm.title("Source").please()
    const note = sourceRealm.note
    const target = targetSearchResult()
    const createdRealm = makeMe.aNoteRealm
      .title("Created relationship")
      .please()
    const createNoteSpy = mockRelationshipNoteCreation(
      sourceRealm,
      createdRealm
    )

    const wrapper = mountAddRelationshipFinalize({
      note,
      targetSearchResult: target,
      seedRealm: sourceRealm,
    })

    await selectRelationType(wrapper, "related to")

    const expectedTitle = formatRelationshipNoteTitle(
      note.noteTopology.title,
      "related to",
      target.noteTopology.title
    )

    expect(createNoteSpy).toHaveBeenCalledTimes(1)
    expect(createNoteSpy).toHaveBeenCalledWith({
      path: { notebook: sourceRealm.notebookRealm.notebook.id },
      body: expect.objectContaining({
        newTitle: expectedTitle,
        content: expect.stringContaining("relation:"),
      }),
    })
    expect(routerReplace).toHaveBeenCalledWith(
      noteShowLocation(createdRealm.id)
    )
    expect(wrapper.emitted().success).toHaveLength(1)
  })
})
