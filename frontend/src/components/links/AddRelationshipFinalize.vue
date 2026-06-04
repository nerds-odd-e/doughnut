<template>
  <div>
    <LoadingModal
      :show="isCreatingRelationshipNote"
      message="Creating relationship note..."
    />
    <span class="daisy-label p-0">Relation note location</span>
    <RadioButtons
      field=""
      scope-name="relationship-placement"
      v-model="formData.relationshipNotePlacement"
      :options="placementOptions"
    />
    <RelationTypeSelect
      field="relationType"
      scope-name="relationship"
      v-model="formData.relationType"
      :error-message="relationshipFormErrors.relationType"
      :inverse-icon="true"
      @update:model-value="relationTypeSelected"
    />
    <div>
      Target:
      <strong
        ><NoteTitleComponent
          v-if="targetSearchResult"
          v-bind="{ noteTopology: targetSearchResult.noteTopology }"
      /></strong>
    </div>
    <button class="daisy-btn daisy-btn-secondary go-back-button" @click="$emit('goBack')">
      <Reply class="w-6 h-6" />
    </button>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import { useRouter } from "vue-router"
import type { Note, NoteSearchResult } from "@generated/doughnut-backend-api"
import RadioButtons from "../form/RadioButtons.vue"
import RelationTypeSelect from "./RelationTypeSelect.vue"
import NoteTitleComponent from "../notes/core/NoteTitleComponent.vue"
import LoadingModal from "../commons/LoadingModal.vue"
import { Reply } from "@lucide/vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import {
  formatRelationshipNoteMarkdown,
  formatRelationshipNoteTitle,
} from "@/utils/relationshipNoteCompose"
import { realmLeafFolder } from "@/components/notes/useNoteSidebarTree"
import {
  resolveRelationshipNoteFolderId,
  type RelationshipNotePlacement,
} from "@/utils/relationshipFolderResolve"

const storageAccessor = useStorageAccessor()
const router = useRouter()

const props = defineProps({
  note: { type: Object as PropType<Note>, required: true },
  targetSearchResult: {
    type: Object as PropType<NoteSearchResult>,
    required: true,
  },
})

const emit = defineEmits(["success", "goBack"])

const placementOptions: {
  value: RelationshipNotePlacement
  label: string
  title: string
}[] = [
  {
    value: "relations_subfolder",
    label: "“relations” subfolder",
    title:
      "Create or use a folder named relations under the folder that contains the source note.",
  },
  {
    value: "same_level_as_source",
    label: "Same level as source",
    title: "Place the relation note in the same folder as the source note.",
  },
  {
    value: "named_after_source_note",
    label: "Folder named like source",
    title: "Create or use a subfolder with the same name as the source note.",
  },
]

const formData = ref<{
  relationType?: string
  relationshipNotePlacement: RelationshipNotePlacement
}>({
  relationType: undefined,
  relationshipNotePlacement: "relations_subfolder",
})

const relationshipFormErrors = ref({
  relationType: undefined as string | undefined,
})

const isCreatingRelationshipNote = ref(false)

const relationTypeSelected = async (relationType: string | undefined) => {
  if (relationType === undefined || isCreatingRelationshipNote.value) return

  isCreatingRelationshipNote.value = true
  try {
    const realm = storageAccessor.value.refOfNoteRealm(props.note.id).value
    const notebookId = realm?.notebookRealm.notebook.id
    if (realm == null || notebookId == null) {
      throw new Error("Missing notebook for source note")
    }

    const sourceNotebookName = realm.notebookRealm.notebook.name
    const sourceFolderId = realmLeafFolder(realm)?.id
    const sourceTitle = props.note.noteTopology.title

    const api = storageAccessor.value.storedApi()
    const folderId = await resolveRelationshipNoteFolderId({
      notebookId,
      sourceFolderId,
      sourceTitle,
      placement: formData.value.relationshipNotePlacement,
    })

    const metaTitle = formatRelationshipNoteTitle(
      sourceTitle,
      relationType,
      props.targetSearchResult.noteTopology.title
    )
    const markdown = formatRelationshipNoteMarkdown({
      relationLabel: relationType,
      sourceEndpoint: {
        title: sourceTitle,
        notebookId,
        notebookName: sourceNotebookName,
      },
      targetEndpoint: {
        title: props.targetSearchResult.noteTopology.title,
        notebookId: props.targetSearchResult.notebookId,
        notebookName: props.targetSearchResult.notebookName,
      },
      relationshipNotebookId: notebookId,
    })

    await api.createRootNoteAtNotebook(
      router,
      notebookId,
      { newTitle: metaTitle, content: markdown },
      {
        folderId: folderId ?? undefined,
        refreshWikiTitleCacheForNoteIds: [
          props.note.id,
          props.targetSearchResult.noteTopology.id,
        ],
      }
    )

    emit("success")
  } catch (e: unknown) {
    const relationTypeError =
      e instanceof Error
        ? ((e as { relationType?: string }).relationType ?? e.message)
        : undefined
    relationshipFormErrors.value = { relationType: relationTypeError }
  } finally {
    isCreatingRelationshipNote.value = false
  }
}
</script>
