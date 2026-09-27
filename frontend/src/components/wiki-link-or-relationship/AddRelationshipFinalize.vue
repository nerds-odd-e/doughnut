<template>
  <div>
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
import type { Note, NoteSearchResult } from "@generated/donut-backend-api"
import RadioButtons from "../form/RadioButtons.vue"
import RelationTypeSelect from "./RelationTypeSelect.vue"
import NoteTitleComponent from "../notes/core/NoteTitleComponent.vue"
import { Reply } from "@lucide/vue"
import { useNoteStore } from "@/store/noteStore"
import { runWithBlockingApiLoading } from "@/managedApi/clientSetup"
import {
  formatRelationshipNoteMarkdown,
  formatRelationshipNoteTitle,
} from "@/utils/relationshipNoteCompose"
import { realmLeafFolder } from "@/components/notes/useNoteSidebarTree"

const noteStore = useNoteStore()
const router = useRouter()

const props = defineProps({
  note: { type: Object as PropType<Note>, required: true },
  targetSearchResult: {
    type: Object as PropType<NoteSearchResult>,
    required: true,
  },
  navigateOnSuccess: { type: Boolean, default: true },
})

const emit = defineEmits(["success", "goBack"])

type RelationshipNotePlacement =
  | "relations_subfolder"
  | "same_level_as_source"
  | "named_after_source_note"

const childFolderNameFor = (
  placement: RelationshipNotePlacement,
  sourceTitle: string
): string | undefined =>
  ({
    relations_subfolder: "relations",
    same_level_as_source: undefined,
    named_after_source_note: sourceTitle,
  })[placement]

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
  if (relationType === undefined) return

  try {
    const realm = noteStore.refOfNoteRealm(props.note.id).value
    const notebookId = realm?.notebookRealm.notebook.id
    if (realm == null || notebookId == null) {
      throw new Error("Missing notebook for source note")
    }

    const sourceNotebookName = realm.notebookRealm.notebook.name
    const sourceFolderId = realmLeafFolder(realm)?.id
    const sourceTitle = props.note.noteTopology.title

    await runWithBlockingApiLoading(async () => {
      const api = noteStore
      const childFolderName = childFolderNameFor(
        formData.value.relationshipNotePlacement,
        sourceTitle
      )

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
        { newTitle: metaTitle, content: markdown, childFolderName },
        {
          folderId: sourceFolderId,
          skipNavigation: props.navigateOnSuccess === false,
        }
      )
    }, "Creating relationship note...")

    emit("success")
  } catch (e: unknown) {
    const relationTypeError =
      e instanceof Error
        ? ((e as { relationType?: string }).relationType ?? e.message)
        : undefined
    relationshipFormErrors.value = { relationType: relationTypeError }
  }
}
</script>
