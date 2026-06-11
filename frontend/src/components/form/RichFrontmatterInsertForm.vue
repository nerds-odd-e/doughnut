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
        <RichFrontmatterImagePropertyValue
          v-else-if="isImagePropertyKey(draftKey)"
          ref="valueInputRef"
          :model-value="draftValue"
          :wiki-titles="wikiTitles"
          :note-id="noteId"
          ariaLabel="Property value"
          value-test-id="rich-note-property-value"
          file-input-test-id="rich-note-image-insert-file-input"
          choose-button-test-id="rich-note-image-insert-choose"
          requires-note-test-id="rich-note-image-insert-requires-note"
          value-wrapper-class="min-w-0 flex-1 basis-48"
          @update:model-value="emit('update:draftValue', $event)"
          @commit="emit('value-blur')"
          @dead-link-click="emit('dead-link-click', $event)"
          @image-upload-state="emit('image-upload-state', $event)"
        />
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
import RichFrontmatterImagePropertyValue from "@/components/form/RichFrontmatterImagePropertyValue.vue"
import RichFrontmatterPropertyExternalLink from "@/components/form/RichFrontmatterPropertyExternalLink.vue"
import RichFrontmatterPropertyKeyPresets from "@/components/form/RichFrontmatterPropertyKeyPresets.vue"
import WikiPropertyValueField from "@/components/form/WikiPropertyValueField.vue"
import type { WikiTitle } from "@generated/doughnut-backend-api"
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
const valueInputRef = ref<{ focus: () => void } | null>(null)

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

defineExpose({ focusValueInput })
</script>
