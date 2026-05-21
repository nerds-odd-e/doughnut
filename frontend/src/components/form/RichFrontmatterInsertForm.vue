<template>
  <div class="mt-1">
    <div
      v-if="insertOpen"
      class="flex flex-wrap gap-2 items-end"
    >
      <label
        class="daisy-form-control w-full sm:w-auto min-w-[8rem]"
      >
        <span class="daisy-label text-xs">Property key</span>
        <div
          class="relative w-full"
          @focusout="onKeyPresetWrapperFocusOut"
        >
          <input
            :id="insertKeyInputId"
            :value="draftKey"
            type="text"
            autocapitalize="off"
            class="daisy-input daisy-input-sm w-full"
            aria-label="Property key"
            :aria-expanded="presetPanelOpen"
            :aria-controls="presetPanelOpen ? insertKeyPresetListId : undefined"
            data-testid="rich-note-property-key"
            @input="onKeyInput"
            @focus="presetPanelOpen = true"
            @keydown.enter.prevent="focusValueInput"
          />
          <RichFrontmatterPropertyKeyPresets
            v-if="presetPanelOpen"
            :list-id="insertKeyPresetListId"
            :property-rows="propertyRows"
            @select="onPresetSelected"
          />
        </div>
      </label>
      <label
        class="daisy-form-control w-full sm:flex-1 min-w-[8rem]"
      >
        <span class="daisy-label text-xs">Property value</span>
        <div
          v-if="isWikidataIdPropertyKey(draftKey)"
          class="flex flex-wrap items-center gap-2"
        >
          <span class="font-mono text-sm">{{
            draftValue.trim() || "—"
          }}</span>
          <RichFrontmatterPropertyExternalLink
            kind="wikidata"
            :value="draftValue"
          />
          <button
            type="button"
            class="daisy-btn daisy-btn-sm daisy-btn-outline"
            data-testid="rich-note-wikidata-property-insert-edit"
            @click="emit('wikidata-dialog-open')"
          >
            Set…
          </button>
        </div>
        <div
          v-else-if="isImagePropertyKey(draftKey)"
          class="flex flex-wrap items-center gap-2"
        >
          <input
            ref="insertImageFileInputRef"
            type="file"
            accept="image/*"
            class="hidden"
            data-testid="rich-note-image-insert-file-input"
            @change="onInsertImageFileSelected"
          />
          <span class="font-mono text-sm">{{
            draftValue.trim() || "—"
          }}</span>
          <RichFrontmatterPropertyExternalLink
            v-if="draftValue.trim()"
            kind="url"
            :value="draftValue"
          />
          <button
            type="button"
            class="daisy-btn daisy-btn-sm daisy-btn-outline"
            data-testid="rich-note-image-insert-choose"
            :disabled="!noteId || insertImageUploading"
            @click="openInsertImageFilePicker"
          >
            {{ draftValue.trim() ? "Replace…" : "Choose image…" }}
          </button>
          <span
            v-if="!noteId"
            class="text-xs text-base-content/70"
            data-testid="rich-note-image-insert-requires-note"
          >
            Save the note before attaching an image.
          </span>
        </div>
        <div
          v-else
          :class="
            isUrlPropertyKey(draftKey)
              ? 'flex min-w-0 items-center gap-2'
              : ''
          "
        >
          <div
            :class="
              isUrlPropertyKey(draftKey) ? 'min-w-0 flex-1' : ''
            "
          >
            <WikiPropertyValueField
              ref="valueInputRef"
              :model-value="draftValue"
              :wiki-titles="wikiTitles"
              aria-label="Property value"
              data-testid="rich-note-property-value"
              @update:model-value="emit('update:draftValue', $event)"
              @blur="emit('value-blur')"
              @dead-link-click="emit('dead-link-click', $event)"
            />
          </div>
          <RichFrontmatterPropertyExternalLink
            v-if="isUrlPropertyKey(draftKey) && draftValue.trim()"
            kind="url"
            :value="draftValue"
          />
        </div>
      </label>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import RichFrontmatterPropertyExternalLink from "@/components/form/RichFrontmatterPropertyExternalLink.vue"
import RichFrontmatterPropertyKeyPresets from "@/components/form/RichFrontmatterPropertyKeyPresets.vue"
import WikiPropertyValueField from "@/components/form/WikiPropertyValueField.vue"
import type { WikiTitle } from "@generated/doughnut-backend-api"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import {
  isImagePropertyKey,
  isUrlPropertyKey,
  isWikidataIdPropertyKey,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"
import type { DeadLinkPayload } from "@/utils/wikiPropertyValueField"

const props = defineProps<{
  insertOpen: boolean
  draftKey: string
  draftValue: string
  wikiTitles: WikiTitle[]
  insertKeyInputId: string
  insertKeyPresetListId: string
  propertyRows: PropertyRow[]
  noteId?: number
}>()

const emit = defineEmits<{
  "update:draftKey": [string]
  "update:draftValue": [string]
  "value-blur": []
  "dead-link-click": [payload: DeadLinkPayload]
  "wikidata-dialog-open": []
  "image-upload-state": [inProgress: boolean]
}>()

const presetPanelOpen = ref(false)
const valueInputRef = ref<InstanceType<typeof WikiPropertyValueField> | null>(
  null
)
const insertImageFileInputRef = ref<HTMLInputElement | null>(null)
const insertImageUploading = ref(false)

function onKeyInput(event: Event) {
  emit("update:draftKey", (event.target as HTMLInputElement).value)
}

function onKeyPresetWrapperFocusOut(event: FocusEvent) {
  const root = event.currentTarget as HTMLElement | null
  const next = event.relatedTarget as Node | null
  if (root?.contains(next)) return
  presetPanelOpen.value = false
}

function onPresetSelected(key: string) {
  emit("update:draftKey", key)
  presetPanelOpen.value = false
  requestAnimationFrame(() => {
    document.getElementById(props.insertKeyInputId)?.focus()
  })
}

function focusValueInput() {
  valueInputRef.value?.focus()
}

function openInsertImageFilePicker() {
  insertImageFileInputRef.value?.click()
}

async function onInsertImageFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ""
  const noteId = props.noteId
  if (!file || noteId === undefined) return

  emit("image-upload-state", true)
  insertImageUploading.value = true
  try {
    const { data, error } = await apiCallWithLoading(() =>
      NoteController.uploadNoteImage({
        path: { note: noteId },
        body: { uploadImage: file },
      })
    )
    if (!error && data?.imagePath) {
      emit("update:draftValue", data.imagePath)
      emit("value-blur")
    }
  } finally {
    insertImageUploading.value = false
    emit("image-upload-state", false)
  }
}

defineExpose({ focusValueInput })
</script>
