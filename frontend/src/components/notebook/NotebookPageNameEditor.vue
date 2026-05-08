<template>
  <h1
    v-if="!editingNotebookName"
    class="daisy-text-xl daisy-font-semibold daisy-text-base-content daisy-m-0 daisy-inline-block daisy-max-w-full"
  >
    <button
      type="button"
      class="daisy-btn daisy-btn-ghost daisy-h-auto daisy-min-h-0 daisy-p-0 daisy-normal-case daisy-text-xl daisy-font-semibold daisy-text-base-content daisy-justify-start daisy-text-left"
      title="Click to rename notebook"
      data-testid="notebook-page-name-edit"
      @click="startEditingNotebookName"
    >
      {{ name }}
    </button>
  </h1>
  <div
    v-else
    class="daisy-flex daisy-flex-col daisy-gap-2 daisy-w-full"
    data-testid="notebook-page-name-edit-row"
    @keydown.escape.prevent="cancelEditingNotebookName"
  >
    <p
      class="daisy-text-sm daisy-text-base-content/80 daisy-m-0"
      data-testid="notebook-page-name-rename-warning"
    >
      If you change this notebook&apos;s name, links from other notebooks to notes here may
      stop working.
    </p>
    <PathNameEditor
      v-model="draftNotebookName"
      :error-message="nameError"
      autofocus
      initial-select-all
      label-text="Notebook name"
      editor-role="textbox"
      placeholder="Notebook name"
      editor-data-test="notebook-page-name-input"
    >
      <template #append>
        <button
          type="button"
          class="daisy-btn daisy-btn-primary"
          data-testid="notebook-page-name-update"
          :disabled="updatingNotebookName"
          @click="submitNotebookNameUpdate"
        >
          {{ updatingNotebookName ? "Updating…" : "Update" }}
        </button>
        <button
          type="button"
          class="daisy-btn daisy-btn-ghost"
          data-testid="notebook-page-name-cancel"
          :disabled="updatingNotebookName"
          @click="cancelEditingNotebookName"
        >
          Cancel
        </button>
      </template>
    </PathNameEditor>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref, watch } from "vue"
import type {
  Notebook,
  NotebookUpdateRequest,
} from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import PathNameEditor from "@/components/notes/core/PathNameEditor.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { toOpenApiError } from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useToast } from "@/composables/useToast"

const props = defineProps({
  notebookId: { type: Number, required: true },
  name: { type: String, required: true },
  settingsBody: {
    type: Object as PropType<NotebookUpdateRequest>,
    required: true,
  },
})

const emit = defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
}>()

const { popups } = usePopups()
const { showSuccessToast } = useToast()

const editingNotebookName = ref(false)
const draftNotebookName = ref("")
const updatingNotebookName = ref(false)
const nameError = ref<string | undefined>(undefined)

watch(
  () => props.notebookId,
  () => {
    editingNotebookName.value = false
  }
)

const startEditingNotebookName = () => {
  nameError.value = undefined
  draftNotebookName.value = props.name ?? ""
  editingNotebookName.value = true
}

const cancelEditingNotebookName = () => {
  editingNotebookName.value = false
  nameError.value = undefined
}

const submitNotebookNameUpdate = async () => {
  const trimmed = draftNotebookName.value.trim()
  if (trimmed === "") {
    nameError.value = "Notebook name cannot be empty"
    return
  }
  const previousName = (props.name ?? "").trim()
  if (trimmed !== previousName) {
    const confirmed = await popups.confirm(
      "If you change this notebook name, links from other notebooks to notes here may stop working. Continue?"
    )
    if (!confirmed) {
      return
    }
  }
  updatingNotebookName.value = true
  nameError.value = undefined
  const { data: updatedNotebook, error } = await apiCallWithLoading(() =>
    NotebookController.updateNotebook({
      path: { notebook: props.notebookId },
      body: { ...props.settingsBody, name: trimmed },
    })
  )
  updatingNotebookName.value = false
  if (!error) {
    emit("notebook-updated", updatedNotebook!)
    showSuccessToast("Notebook name updated")
    editingNotebookName.value = false
  } else {
    const errorObj = toOpenApiError(error)
    nameError.value = errorObj.errors?.name ?? errorObj.message
  }
}
</script>
