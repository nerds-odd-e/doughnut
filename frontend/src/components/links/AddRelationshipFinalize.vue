<template>
  <div>
    <span class="daisy-label daisy-p-0">Relation note location</span>
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
      <Reply class="daisy-w-6 daisy-h-6" />
    </button>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import type { Note, NoteSearchResult } from "@generated/doughnut-backend-api"
import RadioButtons from "../form/RadioButtons.vue"
import RelationTypeSelect from "./RelationTypeSelect.vue"
import NoteTitleComponent from "../notes/core/NoteTitleComponent.vue"
import { Reply } from "lucide-vue-next"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import {
  formatRelationshipNoteMarkdown,
  formatRelationshipNoteTitle,
} from "@/utils/relationshipNoteCompose"
import {
  resolveRelationshipNoteFolderId,
  type RelationshipNotePlacement,
} from "@/utils/relationshipFolderResolve"

const storageAccessor = useStorageAccessor()

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

const relationTypeSelected = async (relationType: string | undefined) => {
  try {
    if (relationType === undefined) return

    const { useRouter } = await import("vue-router")
    const router = useRouter()
    const realm = storageAccessor.value.refOfNoteRealm(props.note.id).value
    const notebookId = realm?.notebookView.notebook.id
    if (realm == null || notebookId == null) {
      throw new Error("Missing notebook for source note")
    }

    const sourceNotebookName = realm.notebookView.notebook.name
    const sourceFolderId = props.note.noteTopology.folderId
    const sourceTitle = props.note.noteTopology.title

    const api = storageAccessor.value.storedApi()
    const folderId = await resolveRelationshipNoteFolderId({
      api,
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

    const created = await api.createRootNoteAtNotebook(
      router,
      notebookId,
      { newTitle: metaTitle, wikidataId: "" },
      {
        folderId: folderId ?? undefined,
        skipRouterReplace: true,
      }
    )

    await api.setNoteContentWithoutUndo(created.id, markdown)
    await api.loadNoteRealm(props.note.id)

    emit("success")
  } catch (res) {
    relationshipFormErrors.value = res as {
      asFirstChild: string | undefined
      relationType: string | undefined
      moveUnder: string | undefined
    }
  }
}
</script>
