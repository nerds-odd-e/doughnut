import {
  NoteController,
  NotebookController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import AddRelationshipFinalize from "@/components/wiki-link-or-relationship/AddRelationshipFinalize.vue"
import type {
  Note,
  NoteRealm,
  NoteSearchResult,
} from "@generated/doughnut-backend-api"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { defineComponent, type PropType } from "vue"
import makeMe from "donut-test-fixtures/makeMe"
import { expect } from "vitest"

export function targetSearchResult(title = "Target"): NoteSearchResult {
  return makeMe.aNoteSearchResult.title(title).notebookId(1).do()
}

export function mountAddRelationshipFinalize({
  note,
  targetSearchResult,
  seedRealm,
  navigateOnSuccess = true,
  withLoadingModal = false,
}: {
  note: Note
  targetSearchResult: NoteSearchResult
  seedRealm?: NoteRealm
  navigateOnSuccess?: boolean
  withLoadingModal?: boolean
}) {
  const Host = defineComponent({
    components: withLoadingModal
      ? { AddRelationshipFinalize, GlobalApiLoadingModal }
      : { AddRelationshipFinalize },
    props: {
      note: { type: Object as PropType<Note>, required: true },
      targetSearchResult: {
        type: Object as PropType<NoteSearchResult>,
        required: true,
      },
      navigateOnSuccess: { type: Boolean, default: true },
    },
    emits: ["success", "goBack"],
    template: `
      <AddRelationshipFinalize
        :note="note"
        :target-search-result="targetSearchResult"
        :navigate-on-success="navigateOnSuccess"
        @success="$emit('success')"
        @goBack="$emit('goBack')"
      />
      ${withLoadingModal ? "<GlobalApiLoadingModal />" : ""}
    `,
  })
  const renderer = helper.component(Host).withCleanStorage()
  if (seedRealm) {
    useStorageAccessor().value.refreshNoteRealm(seedRealm)
  }
  return renderer
    .withProps({ note, targetSearchResult, navigateOnSuccess })
    .mount(withLoadingModal ? { attachTo: document.body } : undefined)
}

export async function selectRelationType(
  wrapper: VueWrapper,
  relationType: string
) {
  const radio = wrapper.find(`[id="relationship-${relationType}"]`)
  expect(radio.exists()).toBe(true)
  await radio.trigger("change")
  await flushPromises()
}

export function mockRelationshipNoteCreation(
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

export function sourceAndCreatedRelationshipRealms() {
  const sourceRealm = makeMe.aNoteRealm.title("Source").please()
  const createdRealm = makeMe.aNoteRealm.title("Created relationship").please()
  return { sourceRealm, note: sourceRealm.note, createdRealm }
}
