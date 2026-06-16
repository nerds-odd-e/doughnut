import type { NoteRealm } from "@generated/doughnut-backend-api"
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
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import type { Note } from "@generated/doughnut-backend-api"
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
export let router: ReturnType<typeof createRouter>
export let deleteNoteSpy: ReturnType<typeof mockSdkService>

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
  afterEach(() => {
    document.body.innerHTML = ""
    vi.clearAllMocks()
    teardownGlobalClientForTesting()
  })

  beforeEach(() => {
    usePopups().popups.register({ popupInfo: [] })
    mockToast.error.mockClear()
    mockToast.warning.mockClear()
    deleteNoteSpy = mockSdkService(NoteController, "deleteNote", undefined)
    router = createRouter({
      history: createWebHistory(),
      routes,
    })
    renderer = helper
      .component(NoteMoreOptionsFormWithGlobalLoading)
      .withRouter(router)
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
