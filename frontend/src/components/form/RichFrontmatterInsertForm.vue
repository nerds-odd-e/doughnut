<template>
  <div class="daisy-mt-1">
    <div
      v-if="insertOpen"
      class="daisy-flex daisy-flex-wrap daisy-gap-2 daisy-items-end"
    >
      <label
        class="daisy-form-control daisy-w-full sm:daisy-w-auto daisy-min-w-[8rem]"
      >
        <span class="daisy-label daisy-text-xs">Property key</span>
        <div
          class="daisy-relative daisy-w-full"
          @focusout="onKeyPresetWrapperFocusOut"
        >
          <input
            :id="insertKeyInputId"
            :value="draftKey"
            type="text"
            autocapitalize="off"
            class="daisy-input daisy-input-bordered daisy-input-sm daisy-w-full"
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
        class="daisy-form-control daisy-w-full sm:daisy-flex-1 daisy-min-w-[8rem]"
      >
        <span class="daisy-label daisy-text-xs">Property value</span>
        <div
          v-if="isWikidataIdPropertyKey(draftKey)"
          class="daisy-flex daisy-flex-wrap daisy-items-center daisy-gap-2"
        >
          <span class="daisy-font-mono daisy-text-sm">{{
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
          class="daisy-flex daisy-flex-wrap daisy-items-center daisy-gap-2"
        >
          <input
            ref="insertImageFileInputRef"
            type="file"
            accept="image/*"
            class="daisy-hidden"
            data-testid="rich-note-image-insert-file-input"
            @change="onInsertImageFileSelected"
          />
          <span class="daisy-font-mono daisy-text-sm">{{
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
            class="daisy-text-xs daisy-text-base-content/70"
            data-testid="rich-note-image-insert-requires-note"
          >
            Save the note before attaching an image.
          </span>
        </div>
        <div
          v-else
          :class="
            isUrlPropertyKey(draftKey)
              ? 'daisy-flex daisy-min-w-0 daisy-items-center daisy-gap-2'
              : ''
          "
        >
          <div
            :class="
              isUrlPropertyKey(draftKey) ? 'daisy-min-w-0 daisy-flex-1' : ''
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
