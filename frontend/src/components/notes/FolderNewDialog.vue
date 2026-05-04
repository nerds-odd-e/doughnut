<template>
  <div class="daisy-card daisy-w-full">
    <div class="daisy-card-body">
      <form @submit.prevent="processForm">
        <fieldset :disabled="processing">
          <div class="daisy-form-control">
            <label class="daisy-label" for="sidebar-new-folder-name">
              <span class="daisy-label-text">Folder name</span>
            </label>
            <input
              id="sidebar-new-folder-name"
              v-model="name"
              v-focus
              type="text"
              class="daisy-input daisy-input-bordered daisy-w-full"
              autocomplete="off"
            />
            <p v-if="nameError" class="daisy-label">
              <span class="daisy-label-text-alt text-error">{{ nameError }}</span>
            </p>
          </div>
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
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { toOpenApiError } from "@/managedApi/openApiError"

const props = defineProps<{
  notebookId: number
  underNoteId?: number
  underFolderId?: number
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
