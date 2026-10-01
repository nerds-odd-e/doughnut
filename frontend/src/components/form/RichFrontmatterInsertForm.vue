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
        <RichFrontmatterPropertyKeyField
          class="w-full"
          :model-value="draftKey"
          :input-id="insertKeyInputId"
          :list-id="insertKeyPresetListId"
          label="Property key"
          test-id="rich-note-property-key"
          :property-rows="propertyRows"
          @update:model-value="emit('update:draftKey', $event)"
          @enter="focusValueInput"
          @select="focusValueInput"
        />
      </label>
      <label
        ref="valueAreaRef"
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
          :model-value="draftValue"
          :note-id="noteId"
          ariaLabel="Property value"
          value-test-id="rich-note-property-value"
          file-input-test-id="rich-note-image-insert-file-input"
          choose-button-test-id="rich-note-image-insert-choose"
          requires-note-test-id="rich-note-image-insert-requires-note"
          value-wrapper-class="min-w-0 flex-1 basis-48"
          @update:model-value="emit('update:draftValue', $event)"
          @enter="emit('add')"
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
            <PropertyValueField
              :model-value="draftValue"
              :wiki-links="wikiLinks"
              :last-saved-markdown="lastSavedMarkdown"
              aria-label="Property value"
              data-testid="rich-note-property-value"
              @update:model-value="emit('update:draftValue', $event)"
              @enter="emit('add')"
              @dead-wiki-link-click="emit('dead-wiki-link-click', $event)"
            />
          </div>
          <RichFrontmatterPropertyExternalLink
            v-if="isUrlPropertyKey(draftKey) && draftValue.trim()"
            kind="url"
            :value="draftValue"
          />
        </div>
      </label>
      <button
        type="button"
        class="daisy-btn daisy-btn-sm daisy-btn-primary"
        data-testid="rich-note-property-insert-add"
        @click="emit('add')"
      >
        Add
      </button>
      <button
        type="button"
        class="daisy-btn daisy-btn-sm daisy-btn-ghost"
        aria-label="Cancel adding property"
        data-testid="rich-note-property-insert-cancel"
        @click="emit('cancel')"
      >
        <X class="h-4 w-4" aria-hidden="true" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { X } from "@lucide/vue"
import { ref } from "vue"
import RichFrontmatterImagePropertyValue from "@/components/form/RichFrontmatterImagePropertyValue.vue"
import RichFrontmatterPropertyExternalLink from "@/components/form/RichFrontmatterPropertyExternalLink.vue"
import RichFrontmatterPropertyKeyField from "@/components/form/RichFrontmatterPropertyKeyField.vue"
import PropertyValueField from "@/components/form/PropertyValueField.vue"
import type { WikiLink } from "@generated/donut-backend-api"
import {
  isImagePropertyKey,
  isUrlPropertyKey,
  isWikidataIdPropertyKey,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"
import { scheduleFocusTargetWithin } from "@/utils/focusTarget"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"

defineProps<{
  insertOpen: boolean
  draftKey: string
  draftValue: string
  wikiLinks: WikiLink[]
  lastSavedMarkdown?: string
  insertKeyInputId: string
  insertKeyPresetListId: string
  propertyRows: PropertyRow[]
  noteId?: number
}>()

const emit = defineEmits<{
  "update:draftKey": [string]
  "update:draftValue": [string]
  add: []
  cancel: []
  "dead-wiki-link-click": [payload: DeadWikiLinkPayload]
  "wikidata-dialog-open": []
  "image-upload-state": [inProgress: boolean]
}>()

const valueAreaRef = ref<HTMLElement | null>(null)

function focusValueInput() {
  scheduleFocusTargetWithin(valueAreaRef.value)
}
</script>
