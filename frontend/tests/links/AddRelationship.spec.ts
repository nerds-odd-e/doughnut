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
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
  testFolderStub,
} from "@tests/helpers"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { defineComponent, nextTick, type PropType } from "vue"
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
  const Host = defineComponent({
    components: { AddRelationshipFinalize, GlobalApiLoadingModal },
    props: {
      note: { type: Object as PropType<Note>, required: true },
      targetSearchResult: {
        type: Object as PropType<NoteSearchResult>,
        required: true,
      },
    },
    emits: ["success", "goBack"],
    template: `
      <AddRelationshipFinalize
        :note="note"
        :target-search-result="targetSearchResult"
        @success="$emit('success')"
        @goBack="$emit('goBack')"
      />
      <GlobalApiLoadingModal />
    `,
  })
  const renderer = helper.component(Host).withCleanStorage()
  if (seedRealm) {
    useStorageAccessor().value.refreshNoteRealm(seedRealm)
  }
  return renderer
    .withProps({ note, targetSearchResult })
    .mount({ attachTo: document.body })
}

async function selectRelationType(wrapper: VueWrapper, relationType: string) {
  await wrapper
    .findComponent(RelationTypeSelect)
    .vm.$emit("update:modelValue", relationType)
  await flushPromises()
}

function mockRelationshipNoteCreation(
  sourceRealm: NoteRealm,
  createdRealm: NoteRealm,
  holdCreate?: Promise<void>
) {
  mockSdkService(NoteController, "showNote", sourceRealm)
  mockSdkService(TextContentController, "updateNoteContent", sourceRealm)
  if (holdCreate) {
    return mockSdkServiceWithImplementation(
      NotebookController,
      "createNoteAtNotebookRoot",
      async () => {
        await holdCreate
        return createdRealm
      }
    )
  }
  return mockSdkService(
    NotebookController,
    "createNoteAtNotebookRoot",
    createdRealm
  )
}

function sourceAndCreatedRelationshipRealms() {
  const sourceRealm = makeMe.aNoteRealm.title("Source").please()
  const createdRealm = makeMe.aNoteRealm.title("Created relationship").please()
  return { sourceRealm, note: sourceRealm.note, createdRealm }
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

  afterEach(() => {
    teardownGlobalClientForTesting()
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
    })

    const selectPromise = selectRelationType(wrapper, "related to")
    await nextTick()

    expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
    expect(document.body.textContent).toContain("Creating relationship note...")

    resolveCreate!()
    await selectPromise

    expect(document.querySelector(".loading-modal-mask")).toBeNull()
  })

  it("creates relationship note, navigates, and emits success", async () => {
    const { sourceRealm, note, createdRealm } =
      sourceAndCreatedRelationshipRealms()
    const target = targetSearchResult()
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
