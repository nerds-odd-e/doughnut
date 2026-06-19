<template>
  <div class="flex min-w-0 flex-wrap items-center gap-2">
    <input
      ref="imageFileInputRef"
      type="file"
      accept="image/*"
      class="hidden"
      :data-testid="fileInputTestId"
      @change="onImageFileSelected"
    />
    <input
      ref="valueInputRef"
      type="text"
      class="daisy-input daisy-input-sm w-full"
      :class="valueWrapperClass"
      :value="modelValue"
      :title="modelValue.trim()"
      :aria-label="ariaLabel"
      :data-testid="valueTestId"
      placeholder="Full url of existing image."
      autocapitalize="off"
      autocomplete="off"
      @input="onValueInput"
      @focus="emit('focus')"
      @blur="emit('commit')"
    />
    <RichFrontmatterPropertyExternalLink
      v-if="modelValue.trim()"
      kind="url"
      :value="modelValue"
    />
    <button
      type="button"
      class="daisy-btn daisy-btn-sm daisy-btn-outline shrink-0"
      :data-testid="chooseButtonTestId"
      :disabled="!noteId || imageUploading"
      @click="openImageFilePicker"
    >
      {{ modelValue.trim() ? "Replace…" : "Choose image…" }}
    </button>
    <span
      v-if="!noteId"
      class="text-xs text-base-content/70"
      :data-testid="requiresNoteTestId"
    >
      Save the note before uploading an image.
    </span>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import RichFrontmatterPropertyExternalLink from "@/components/form/RichFrontmatterPropertyExternalLink.vue"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const props = withDefaults(
  defineProps<{
    modelValue: string
    noteId?: number
    ariaLabel: string
    valueTestId: string
    fileInputTestId: string
    chooseButtonTestId: string
    requiresNoteTestId: string
    valueWrapperClass?: string
  }>(),
  { valueWrapperClass: "min-w-0 flex-1" }
)

const emit = defineEmits<{
  "update:modelValue": [value: string]
  focus: []
  commit: []
  "image-upload-state": [inProgress: boolean]
}>()

const valueInputRef = ref<HTMLInputElement | null>(null)
const imageFileInputRef = ref<HTMLInputElement | null>(null)
const imageUploading = ref(false)

function onValueInput(event: Event) {
  emit("update:modelValue", (event.target as HTMLInputElement).value)
}

function openImageFilePicker() {
  imageFileInputRef.value?.click()
}

async function onImageFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ""
  const noteId = props.noteId
  if (!file || noteId === undefined) return

  emit("image-upload-state", true)
  imageUploading.value = true
  try {
    const { data, error } = await apiCallWithLoading(() =>
      NoteController.uploadNoteImage({
        path: { note: noteId },
        body: { uploadImage: file },
      })
    )
    if (!error && data?.imagePath) {
      emit("update:modelValue", data.imagePath)
      emit("commit")
    }
  } finally {
    imageUploading.value = false
    emit("image-upload-state", false)
  }
}

defineExpose({
  focus: () => valueInputRef.value?.focus(),
})
</script>
