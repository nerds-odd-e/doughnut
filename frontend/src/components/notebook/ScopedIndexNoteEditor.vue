<template>
  <div
    class="scoped-index-note-editor"
    :class="[
      { dirty: isDirty, 'scoped-index-note-editor--flush': flush },
      flush ? '' : 'mb-6',
    ]"
    :data-testid="`${testIdPrefix}-body`"
  >
    <h2
      v-if="headingLabel"
      class="text-lg font-semibold text-base-content mb-2"
    >
      {{ headingLabel }}
    </h2>
    <div :data-testid="`${testIdPrefix}-editor`">
      <RichMarkdownEditor
        :model-value="localValue"
        :multiple-line="true"
        :scope-name="richEditorScopeName"
        field="content"
        :readonly="false"
        :wiki-titles="[]"
        :is-index-context="true"
        @update:model-value="propose"
        @blur="flushAutosave"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useDebouncedTextAutosave } from "@/composables/useDebouncedTextAutosave"
import { normalizeNoteContent } from "@/utils/normalizeNoteContent"
import { hasNewWikiLinkTexts } from "@/utils/noteContentWikiLinks"
import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue"

const props = withDefaults(
  defineProps<{
    notebookId: number
    folderId?: number | null
    indexContent?: string | null
    testIdPrefix?: string
    richEditorScopeName?: string
    headingLabel?: string
    /** No nested card chrome — full column width, no top margin (notebook Home). */
    flush?: boolean
  }>(),
  {
    testIdPrefix: "notebook-index",
    richEditorScopeName: "notebook-index",
    headingLabel: "Index",
    flush: false,
  }
)

const emit = defineEmits<{
  (e: "saved"): void
}>()

const persistIndexContent = async (content: string) => {
  if (props.folderId != null) {
    await apiCallWithLoading(() =>
      NotebookController.updateFolderIndexContent({
        path: { notebook: props.notebookId, folder: props.folderId! },
        body: { content },
      })
    )
  } else {
    await apiCallWithLoading(() =>
      NotebookController.updateNotebookIndexContent({
        path: { notebook: props.notebookId },
        body: { content },
      })
    )
  }
  emit("saved")
}

const {
  localValue,
  isDirty,
  propose,
  flush: flushAutosave,
} = useDebouncedTextAutosave({
  externalValue: () => props.indexContent ?? undefined,
  persist: persistIndexContent,
  normalize: normalizeNoteContent,
  shouldFlushImmediately: (prev, next) => hasNewWikiLinkTexts(prev, next),
})
</script>

<style scoped>
.scoped-index-note-editor:not(.scoped-index-note-editor--flush) {
  background: color-mix(in oklch, var(--color-base-200) 80%, transparent);
  border-radius: 8px;
  padding: 1rem 1.25rem;
}

.scoped-index-note-editor--flush {
  margin-top: 0;
  padding: 0;
  background: transparent;
  border-radius: 0;
}
</style>
