import type { Note, NoteRealm } from "@generated/doughnut-backend-api"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import NoteMoreOptionsForm from "@/components/notes/widgets/NoteMoreOptionsForm.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import usePopups from "@/components/commons/Popups/usePopups"
import { wikiTitleFromInnerAndNoteId } from "@/utils/wikiPropertyValueField"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { createMemoryHistory, createRouter } from "vue-router"
import { afterEach, beforeEach, vi } from "vitest"
import { defineComponent, type PropType } from "vue"
import { relationshipNoteContent } from "./relationshipNoteTestContent"

export const mockToast = {
  error: vi.fn(),
  warning: vi.fn(),
}

vi.mock("vue-toastification", () => ({
  useToast: () => mockToast,
}))

export const noteMoreOptionsDeleteFormNote = makeMe.aNote.please()
export let deleteNoteSpy: ReturnType<typeof mockSdkService>

export const noteMoreOptionsDeleteFormRouter = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: "/", component: { template: "<div />" } },
    {
      path: "/notebooks/:notebookId/folders/:folderId",
      name: "folderPage",
      component: { template: "<div />" },
      props: true,
    },
    {
      path: "/notebooks/:notebookId",
      name: "notebookPage",
      component: { template: "<div />" },
      props: true,
    },
    {
      path: "/n:noteId",
      name: "noteShow",
      component: { template: "<div />" },
      props: true,
    },
  ],
})

export const loadingModalMask = () =>
  document.querySelector(".loading-modal-mask")

export function deleteNoteButton(wrapper: VueWrapper) {
  return wrapper.find('button[title="Delete note (d)"]')
}

export async function clickDeleteNote(wrapper: VueWrapper) {
  await deleteNoteButton(wrapper).trigger("click")
  await flushPromises()
}

export async function mountDeleteFormReady(note: Note) {
  const wrapper = renderer.withProps({ note }).mount()
  await flushPromises()
  return wrapper
}

const NoteMoreOptionsFormWithGlobalLoading = defineComponent({
  components: { GlobalApiLoadingModal, NoteMoreOptionsForm },
  props: {
    note: { type: Object as PropType<Note>, required: true },
  },
  emits: ["close-dialog"],
  template: `
    <NoteMoreOptionsForm
      :note="note"
      @close-dialog="$emit('close-dialog')"
    />
    <GlobalApiLoadingModal />
  `,
})

export let renderer: RenderingHelper<
  typeof NoteMoreOptionsFormWithGlobalLoading
>

export function setupNoteMoreOptionsDeleteFormTests() {
  afterEach(async () => {
    await flushPromises()
    document.body.innerHTML = ""
    vi.clearAllMocks()
    teardownGlobalClientForTesting()
  })

  beforeEach(() => {
    usePopups().popups.register({ popupInfo: [] })
    mockToast.error.mockClear()
    mockToast.warning.mockClear()
    deleteNoteSpy = mockSdkService(NoteController, "deleteNote", undefined)
    renderer = helper
      .component(NoteMoreOptionsFormWithGlobalLoading)
      .withRouter(noteMoreOptionsDeleteFormRouter)
      .withCleanStorage()
  })
}

export function qualifyingRelationRealmForDelete(options?: {
  moonId?: number
  earthId?: number
  relationId?: number
}): {
  moonId: number
  earthId: number
  relationRealm: NoteRealm
} {
  const moonId = options?.moonId ?? 501
  const earthId = options?.earthId ?? 502
  const relationRealm = {
    ...makeMe.aNoteRealm
      .content(relationshipNoteContent("a-part-of", "[[Moon]]", "[[Earth]]"))
      .please(),
    ...(options?.relationId === undefined ? {} : { id: options.relationId }),
    wikiTitles: [
      wikiTitleFromInnerAndNoteId("Moon", moonId),
      wikiTitleFromInnerAndNoteId("Earth", earthId),
    ],
  }

  return { moonId, earthId, relationRealm }
}

export function seedRelationRealmWithInboundReferences(
  relationRealm: NoteRealm
): void {
  useStorageAccessor().value.refreshNoteRealm({
    ...relationRealm,
    references: [makeMe.aNoteRealm.please().note.noteTopology],
  })
}

export function relationNotesForPropChangeTest(options?: {
  moonId?: number
  relationId?: number
}): {
  moonId: number
  relationId: number
  moonNote: Note
  relationNote: Note
} {
  const moonId = options?.moonId ?? 501
  const relationId = options?.relationId ?? 503
  const moonNote = {
    ...makeMe.aNote.please(),
    id: moonId,
    noteTopology: {
      ...makeMe.aNote.please().noteTopology,
      id: moonId,
      title: "Moon",
    },
  }
  const { relationRealm } = qualifyingRelationRealmForDelete({
    moonId,
    relationId,
  })
  const relationNote = {
    ...relationRealm.note,
    id: relationId,
    noteTopology: {
      ...relationRealm.note.noteTopology,
      id: relationId,
    },
  }
  useStorageAccessor().value.refreshNoteRealm({
    ...makeMe.aNoteRealm.title("Moon").please(),
    id: moonId,
  })
  useStorageAccessor().value.refreshNoteRealm(relationRealm)

  return { moonId, relationId, moonNote, relationNote }
}

export async function mountDeleteFormWithNotePropChange(
  moonNote: Note,
  relationNote: Note
) {
  const wrapper = renderer.withProps({ note: moonNote }).mount()
  await flushPromises()
  await wrapper.setProps({ note: relationNote })
  await flushPromises()
  return wrapper
}
