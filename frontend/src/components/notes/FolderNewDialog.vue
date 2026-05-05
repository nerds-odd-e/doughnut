<template>
  <div class="daisy-card daisy-w-full">
    <div class="daisy-card-body">
      <form @submit.prevent="processForm">
        <fieldset :disabled="processing">
          <p
            v-if="parentLocationDescription"
            class="daisy-text-sm daisy-opacity-80 daisy-mb-3"
            data-testid="folder-new-dialog-parent-location"
          >
            {{ parentLocationDescription }}
          </p>
          <PathNameEditor
            v-model="name"
            :error-message="nameError"
            autofocus
            label-text="Folder name"
            editor-role="textbox"
            placeholder="Folder name"
            editor-data-test="folder-name"
          />
          <input
            type="submit"
            value="Submit"
            class="daisy-btn daisy-btn-primary daisy-mt-4"
          />
        </fieldset>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import PathNameEditor from "@/components/notes/core/PathNameEditor.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { toOpenApiError } from "@/managedApi/openApiError"

const props = defineProps<{
  notebookId: number
  underNoteId?: number
  underFolderId?: number
  parentLocationDescription?: string
}>()

const emit = defineEmits<{
  closeDialog: []
}>()

const storageAccessor = useStorageAccessor()
const name = ref("")
const nameError = ref<string | undefined>(undefined)
const processing = ref(false)

const processForm = async () => {
  if (processing.value) return
  processing.value = true
  nameError.value = undefined
  try {
    await storageAccessor.value.storedApi().createFolder(props.notebookId, {
      name: name.value,
      ...(props.underFolderId != null
        ? { underFolderId: props.underFolderId }
        : props.underNoteId != null
          ? { underNoteId: props.underNoteId }
          : {}),
    })
    emit("closeDialog")
  } catch (res: unknown) {
    nameError.value = toOpenApiError(res).message ?? "Failed to create folder"
  } finally {
    processing.value = false
  }
}
</script>
