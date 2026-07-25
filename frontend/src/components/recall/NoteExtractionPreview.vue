<template>
  <div class="text-base" data-test-id="extraction-preview">
    <div class="font-semibold mb-3 text-accent-content">Extract preview:</div>

    <div
      v-if="createError"
      class="daisy-alert daisy-alert-error mb-3 text-sm"
      data-test-id="extraction-preview-error"
    >
      {{ createError }}
    </div>

    <div class="mb-3 text-accent-content">
      <span class="font-medium">Updated original note content</span>
      <div
        class="daisy-tabs daisy-tabs-box mt-1 mb-2"
        role="tablist"
        aria-label="Updated original note content view"
      >
        <button
          type="button"
          class="daisy-tab"
          :class="{ 'daisy-tab-active': originalContentTab === 'content' }"
          role="tab"
          :aria-selected="originalContentTab === 'content'"
          data-test-id="extraction-preview-original-tab-content"
          @click="originalContentTab = 'content'"
        >
          Content
        </button>
        <button
          type="button"
          class="daisy-tab"
          :class="{ 'daisy-tab-active': originalContentTab === 'diff' }"
          role="tab"
          :aria-selected="originalContentTab === 'diff'"
          data-test-id="extraction-preview-original-tab-diff"
          @click="originalContentTab = 'diff'"
        >
          Diff
        </button>
      </div>
      <textarea
        v-if="originalContentTab === 'content'"
        v-model="preview.updatedOriginalNoteContent"
        data-test-id="extraction-preview-original-content"
        class="daisy-textarea daisy-textarea-bordered w-full min-h-24"
      />
      <DiffView
        v-else
        :current="originalNoteContent"
        :old="preview.updatedOriginalNoteContent"
        current-label="Original"
        old-label="Updated"
        max-height="240px"
      />
    </div>

    <label class="block mb-3 text-accent-content">
      <span class="font-medium">New note title</span>
      <textarea
        v-model="preview.newNoteTitle"
        data-test-id="extraction-preview-new-title"
        class="daisy-textarea daisy-textarea-bordered mt-1 w-full"
        rows="1"
      />
    </label>

    <label class="block mb-3 text-accent-content">
      <span class="font-medium">New note content</span>
      <textarea
        v-model="preview.newNoteContent"
        data-test-id="extraction-preview-new-content"
        class="daisy-textarea daisy-textarea-bordered mt-1 w-full min-h-24"
      />
    </label>

    <div class="flex gap-2 mt-4">
      <button
        data-test-id="extraction-preview-back"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        @click="$emit('back')"
      >
        Back
      </button>
      <button
        data-test-id="retry-extraction-preview"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        @click="$emit('retry')"
      >
        Ask AI to retry
      </button>
      <button
        data-test-id="extraction-preview-create"
        class="daisy-btn daisy-btn-primary daisy-btn-sm"
        :disabled="!canCreate"
        @click="$emit('create')"
      >
        Create note
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { NoteExtractionResult } from "@generated/doughnut-backend-api"
import DiffView from "@/components/commons/DiffView.vue"
import { ref } from "vue"

defineProps<{
  originalNoteContent: string
  createError: string
  canCreate: boolean
}>()

defineEmits<{
  back: []
  retry: []
  create: []
}>()

const preview = defineModel<NoteExtractionResult>({ required: true })
const originalContentTab = ref<"content" | "diff">("content")
</script>
