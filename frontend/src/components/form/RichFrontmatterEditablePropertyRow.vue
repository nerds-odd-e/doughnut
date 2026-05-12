<template>
  <div
    class="daisy-grid daisy-grid-cols-[minmax(8rem,auto)_minmax(0,1fr)_auto] daisy-gap-x-4 daisy-gap-y-1 daisy-items-center"
    data-testid="rich-note-property-row"
    :data-row-index="idx"
    :data-property-key="modelValue.key"
  >
    <div
      class="daisy-relative daisy-min-w-[8rem]"
      @focusout="onKeyPresetWrapperFocusOut"
    >
      <input
        :id="keyInputId"
        :value="modelValue.key"
        type="text"
        autocapitalize="off"
        class="daisy-input daisy-input-bordered daisy-input-sm daisy-w-full daisy-min-w-[8rem]"
        :aria-label="`Existing note property key (row ${idx + 1})`"
        :aria-expanded="presetPanelOpen"
        :aria-controls="presetPanelOpen ? presetListId : undefined"
        data-testid="rich-note-property-row-key-input"
        @input="onKeyInput"
        @focus="onKeyFocus"
        @blur="emit('commit')"
      />
      <RichFrontmatterPropertyKeyPresets
        v-if="presetPanelOpen"
        :list-id="presetListId"
        :property-rows="propertyRows"
        @select="onPresetSelected"
      />
    </div>
    <RelationTypeSelectCompact
      v-if="isRelationPropertyKey(modelValue.key)"
      field="relationType"
      scope-name="rich-note-relation-property"
      hide-label
      :model-value="relationModelValue"
      :inverse-icon="true"
      @update:model-value="emit('relation-type-selected', $event)"
    />
    <div
      v-else-if="isImagePropertyKey(modelValue.key)"
      class="daisy-flex daisy-min-w-0 daisy-items-center daisy-gap-2"
    >
      <input
        ref="imageFileInputRef"
        type="file"
        accept="image/*"
        class="daisy-hidden"
        data-testid="rich-note-image-property-file-input"
        @change="onImageFileSelected"
      />
      <template v-if="modelValue.value.trim()">
        <button
          type="button"
          class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-h-auto daisy-min-h-0 daisy-min-w-0 daisy-max-w-full daisy-shrink daisy-truncate daisy-justify-start daisy-py-0.5 daisy-px-1 daisy-font-mono daisy-text-sm daisy-font-normal daisy-text-base-content/90 daisy-normal-case"
          :title="modelValue.value.trim()"
          data-testid="rich-note-image-property-path"
        >
          {{ modelValue.value.trim() }}
        </button>
        <RichFrontmatterPropertyExternalLink
          kind="url"
          :value="modelValue.value"
        />
      </template>
      <template v-else>
        <span
          class="daisy-truncate daisy-font-mono daisy-text-sm daisy-text-base-content/90"
          aria-hidden="true"
          >—</span
        >
      </template>
      <button
        type="button"
        class="daisy-btn daisy-btn-sm daisy-btn-outline daisy-shrink-0"
        data-testid="rich-note-image-property-choose"
        :disabled="!noteId || imageUploading"
        @click="openImageFilePicker"
      >
        {{ modelValue.value.trim() ? "Replace…" : "Choose image…" }}
      </button>
      <span
        v-if="!noteId"
        class="daisy-text-xs daisy-text-base-content/70"
        data-testid="rich-note-image-upload-requires-note"
      >
        Save the note before attaching an image.
      </span>
    </div>
    <div
      v-else-if="isWikidataIdPropertyKey(modelValue.key)"
      class="daisy-flex daisy-min-w-0 daisy-items-center daisy-gap-2"
      :class="modelValue.value.trim() ? '' : 'daisy-justify-between'"
    >
      <template v-if="modelValue.value.trim()">
        <button
          type="button"
          class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-h-auto daisy-min-h-0 daisy-min-w-0 daisy-max-w-full daisy-shrink daisy-truncate daisy-justify-start daisy-py-0.5 daisy-px-1 daisy-font-mono daisy-text-sm daisy-font-normal daisy-text-base-content/90 daisy-normal-case"
          :title="modelValue.value.trim()"
          data-testid="rich-note-wikidata-property-edit"
          :aria-label="`Edit Wikidata ID ${modelValue.value.trim()}`"
          @click="emit('wikidata-dialog-open')"
        >
          {{ modelValue.value.trim() }}
        </button>
        <RichFrontmatterPropertyExternalLink
          kind="wikidata"
          :value="modelValue.value"
        />
      </template>
      <template v-else>
        <span
          class="daisy-truncate daisy-font-mono daisy-text-sm daisy-text-base-content/90"
          aria-hidden="true"
          >—</span
        >
        <button
          type="button"
          class="daisy-btn daisy-btn-sm daisy-btn-outline daisy-shrink-0"
          data-testid="rich-note-wikidata-property-edit"
          aria-label="Set Wikidata ID"
          @click="emit('wikidata-dialog-open')"
        >
          Set…
        </button>
      </template>
    </div>
    <div
      v-else
      class="daisy-min-w-0"
      :class="
        isUrlPropertyKey(modelValue.key)
          ? 'daisy-flex daisy-items-center daisy-gap-2'
          : ''
      "
    >
      <div
        :class="
          isUrlPropertyKey(modelValue.key) ? 'daisy-min-w-0 daisy-flex-1' : ''
        "
      >
        <WikiPropertyValueField
          :model-value="modelValue.value"
          :wiki-titles="wikiTitles"
          :aria-label="`Existing note property value (row ${idx + 1})`"
          data-testid="rich-note-property-row-value-input"
          @update:model-value="onValueUpdate"
          @focus="emit('row-focus')"
          @blur="emit('commit')"
          @dead-link-click="emit('dead-link-click', $event)"
        />
      </div>
      <RichFrontmatterPropertyExternalLink
        v-if="isUrlPropertyKey(modelValue.key) && modelValue.value.trim()"
        kind="url"
        :value="modelValue.value"
      />
    </div>
    <button
      type="button"
      class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-square daisy-shrink-0"
      :aria-label="`Remove note property ${modelValue.key}`"
      data-testid="rich-note-property-row-remove"
      @click="emit('remove')"
    >
      <Minus class="daisy-h-4 daisy-w-4" aria-hidden="true" />
    </button>
  </div>
</template>

<script setup lang="ts">
import { Minus } from "lucide-vue-next"
import { computed, ref } from "vue"
import RichFrontmatterPropertyExternalLink from "@/components/form/RichFrontmatterPropertyExternalLink.vue"
import RichFrontmatterPropertyKeyPresets from "@/components/form/RichFrontmatterPropertyKeyPresets.vue"
import WikiPropertyValueField from "@/components/form/WikiPropertyValueField.vue"
import RelationTypeSelectCompact from "@/components/links/RelationTypeSelectCompact.vue"
import type { WikiTitle } from "@generated/doughnut-backend-api"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import {
  isImagePropertyKey,
  isRelationPropertyKey,
  isUrlPropertyKey,
  isWikidataIdPropertyKey,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"
import type { DeadLinkPayload } from "@/utils/wikiPropertyValueField"
import {
  isKnownRelationKebab,
  relationTypeFromKebab,
} from "@/models/relationTypeOptions"

const props = defineProps<{
  modelValue: PropertyRow
  idx: number
  wikiTitles: WikiTitle[]
  keyInputId: string
  presetListId: string
  propertyRows: PropertyRow[]
  noteId?: number
}>()

const emit = defineEmits<{
  "update:modelValue": [row: PropertyRow]
  "row-focus": []
  commit: []
  remove: []
  "wikidata-dialog-open": []
  "dead-link-click": [payload: DeadLinkPayload]
  "relation-type-selected": [type: string | undefined]
  "image-upload-state": [inProgress: boolean]
}>()

const presetPanelOpen = ref(false)
const imageFileInputRef = ref<HTMLInputElement | null>(null)
const imageUploading = ref(false)

const relationModelValue = computed(() => {
  const v = props.modelValue.value
  if (isKnownRelationKebab(v)) return relationTypeFromKebab(v)
  return v.trim()
})

function onKeyInput(event: Event) {
  emit("update:modelValue", {
    ...props.modelValue,
    key: (event.target as HTMLInputElement).value,
  })
}

function onValueUpdate(value: string) {
  emit("update:modelValue", { ...props.modelValue, value })
}

function onKeyFocus() {
  presetPanelOpen.value = true
  emit("row-focus")
}

function onKeyPresetWrapperFocusOut(event: FocusEvent) {
  const root = event.currentTarget as HTMLElement | null
  const next = event.relatedTarget as Node | null
  if (root?.contains(next)) return
  presetPanelOpen.value = false
}

function onPresetSelected(key: string) {
  emit("update:modelValue", { ...props.modelValue, key })
  presetPanelOpen.value = false
  requestAnimationFrame(() => {
    document.getElementById(props.keyInputId)?.focus()
  })
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
      emit("update:modelValue", {
        ...props.modelValue,
        value: data.imagePath,
      })
      emit("commit")
    }
  } finally {
    imageUploading.value = false
    emit("image-upload-state", false)
  }
}
</script>
