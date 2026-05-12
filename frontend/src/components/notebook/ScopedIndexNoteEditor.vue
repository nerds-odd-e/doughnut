<template>
  <div
    class="scoped-index-note-editor daisy-mb-6"
    :data-testid="`${testIdPrefix}-body`"
  >
    <h2 class="daisy-text-lg daisy-font-semibold daisy-text-base-content daisy-mb-2">
      {{ headingLabel }}
    </h2>
    <div :data-testid="`${testIdPrefix}-editor`">
      <RichMarkdownEditor
        v-model="draftContent"
        :multiple-line="true"
        :scope-name="richEditorScopeName"
        field="content"
        :readonly="false"
        :wiki-titles="[]"
        :is-index-context="true"
      />
    </div>
    <button
      type="button"
      class="daisy-btn daisy-btn-primary daisy-btn-sm daisy-mt-3"
      :data-testid="`${testIdPrefix}-save`"
      :disabled="saving"
      @click="save"
    >
      {{ saving ? saveButtonSavingLabel : saveButtonIdleLabel }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useToast } from "@/composables/useToast"
import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue"

const props = withDefaults(
  defineProps<{
    notebookId: number
    folderId?: number | null
    indexContent?: string | null
    testIdPrefix?: string
    richEditorScopeName?: string
    headingLabel?: string
    saveButtonIdleLabel?: string
    saveButtonSavingLabel?: string
    successToastSaved?: string
  }>(),
  {
    testIdPrefix: "notebook-index",
    richEditorScopeName: "notebook-index",
    headingLabel: "Index",
    saveButtonIdleLabel: "Save index",
    saveButtonSavingLabel: "Saving…",
    successToastSaved: "Index saved",
  }
)

const emit = defineEmits<{
  (e: "saved"): void
}>()

const { showSuccessToast } = useToast()

const draftContent = ref(props.indexContent ?? "")

watch(
  () => [props.notebookId, props.folderId ?? null, props.indexContent] as const,
  ([, , content]) => {
    draftContent.value = content ?? ""
  }
)

const saving = ref(false)

const save = async () => {
  saving.value = true
  try {
    if (props.folderId != null) {
      await apiCallWithLoading(() =>
        NotebookController.updateFolderIndexContent({
          path: { notebook: props.notebookId, folder: props.folderId! },
          body: { content: draftContent.value },
        })
      )
    } else {
      await apiCallWithLoading(() =>
        NotebookController.updateNotebookIndexContent({
          path: { notebook: props.notebookId },
          body: { content: draftContent.value },
        })
      )
    }
    showSuccessToast(props.successToastSaved)
    emit("saved")
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.scoped-index-note-editor {
  background: oklch(var(--b2) / 0.8);
  border-radius: 8px;
  padding: 1rem 1.25rem;
}
</style>
