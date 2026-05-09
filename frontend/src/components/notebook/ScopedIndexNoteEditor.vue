<template>
  <div
    v-if="indexNoteStatus === 'pending'"
    class="scoped-index-note-editor daisy-mb-6 daisy-flex daisy-items-center daisy-gap-2"
    :data-testid="`${testIdPrefix}-loading`"
  >
    <span class="daisy-loading daisy-loading-spinner daisy-loading-sm" />
    <span class="daisy-text-sm daisy-text-base-content/70">{{ loadingHint }}</span>
  </div>

  <div
    v-else-if="indexNoteStatus === 'present' && indexRealm && indexNoteId != null"
    class="scoped-index-note-editor daisy-mb-6"
    :data-testid="`${testIdPrefix}-body`"
  >
    <h2 class="daisy-text-lg daisy-font-semibold daisy-text-base-content daisy-mb-2">
      {{ indexDisplayTitle }}
    </h2>
    <NoteEditableContent
      :note-id="indexNoteId"
      :note-content="indexRealm.note.content ?? ''"
      :readonly="false"
      :as-markdown="false"
      :wiki-titles="indexRealm.wikiTitles ?? []"
      :note-title-for-wikidata-search="indexRealm.note.noteTopology.title ?? ''"
      :data-testid="`${testIdPrefix}-editable-content`"
      @dead-link-click="pendingDeadLinkTitle = $event"
    />
    <NoteDeadLinkCreateModal
      v-model="pendingDeadLinkTitle"
      :notebook-id="notebookId"
      :note-realm="indexRealm"
      :source-note-id="indexRealm.id"
    />
  </div>

  <div
    v-else-if="indexNoteStatus === 'absent'"
    class="scoped-index-note-editor daisy-mb-6"
    :data-testid="`${testIdPrefix}-body`"
  >
    <h2 class="daisy-text-lg daisy-font-semibold daisy-text-base-content daisy-mb-2">
      {{ absentHeading }}
    </h2>
    <p class="daisy-text-sm daisy-text-base-content/70 daisy-mb-3">
      {{ absentHelpBefore }}
      <span class="daisy-font-mono">{{ indexTitleWord }}</span>{{ absentHelpAfter }}
    </p>
    <div :data-testid="`${testIdPrefix}-draft-editor`">
      <RichMarkdownEditor
        v-model="indexDraftMarkdown"
        :multiple-line="true"
        :scope-name="richEditorScopeName"
        field="content"
        :readonly="false"
        :wiki-titles="[]"
      />
    </div>
    <button
      type="button"
      class="daisy-btn daisy-btn-primary daisy-btn-sm daisy-mt-3"
      :data-testid="`${testIdPrefix}-create-save`"
      :disabled="savingIndexDraft"
      @click="saveIndexDraft"
    >
      {{ savingIndexDraft ? saveButtonSavingLabel : saveButtonIdleLabel }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from "vue"
import { useRouter } from "vue-router"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { useToast } from "@/composables/useToast"
import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue"
import NoteEditableContent from "@/components/notes/core/NoteEditableContent.vue"
import NoteDeadLinkCreateModal from "@/components/notes/NoteDeadLinkCreateModal.vue"

const props = withDefaults(
  defineProps<{
    notebookId: number
    /** When set, create-note request includes this folder id. */
    folderId?: number | null
    indexNoteStatus: "pending" | "present" | "absent"
    indexNoteId?: number
    fetchPage: () => Promise<void>
    testIdPrefix?: string
    richEditorScopeName?: string
    loadingHint?: string
    absentHeading?: string
    absentHelpBefore?: string
    indexTitleWord?: string
    absentHelpAfter?: string
    presentTitleFallback?: string
    saveButtonIdleLabel?: string
    saveButtonSavingLabel?: string
    successToastSaved?: string
    successToastAfterRace?: string
    errorToastAfterRace?: string
  }>(),
  {
    testIdPrefix: "notebook-index",
    richEditorScopeName: "notebook-index",
    loadingHint: "Loading index…",
    absentHeading: "Index",
    absentHelpBefore:
      "No index note yet. Edit below and save to create a root note titled ",
    indexTitleWord: "index",
    absentHelpAfter: ".",
    presentTitleFallback: "Index",
    saveButtonIdleLabel: "Save index",
    saveButtonSavingLabel: "Saving…",
    successToastSaved: "Notebook index saved",
    successToastAfterRace: "Notebook index is now available",
    errorToastAfterRace:
      "Could not create index: a conflicting note may exist. Refresh the page and try again.",
  }
)

const emit = defineEmits<{
  (e: "index-note-created"): void
}>()

const storageAccessor = useStorageAccessor()
const { showSuccessToast, showErrorToast } = useToast()
const router = useRouter()

const indexRealm = computed(() => {
  if (props.indexNoteStatus !== "present" || props.indexNoteId == null) {
    return undefined
  }
  return storageAccessor.value.refOfNoteRealm(props.indexNoteId).value
})

const indexDisplayTitle = computed(() => {
  const t = indexRealm.value?.note.noteTopology.title.trim()
  return t && t.length > 0 ? t : props.presentTitleFallback
})

const indexDraftMarkdown = ref("")
const savingIndexDraft = ref(false)
const pendingDeadLinkTitle = ref<string | null>(null)

watch(
  () =>
    [props.notebookId, props.folderId ?? null, props.indexNoteStatus] as const,
  ([, , status]) => {
    if (status === "absent") {
      indexDraftMarkdown.value = ""
    }
  }
)

const saveIndexDraft = async () => {
  savingIndexDraft.value = true
  try {
    const createOptions: {
      skipRouterReplace: true
      folderId?: number | null
    } = { skipRouterReplace: true }
    if (props.folderId != null) {
      createOptions.folderId = props.folderId
    }
    await storageAccessor.value.storedApi().createRootNoteAtNotebook(
      router,
      props.notebookId,
      {
        newTitle: "index",
        content: indexDraftMarkdown.value,
      },
      createOptions
    )
    showSuccessToast(props.successToastSaved)
    emit("index-note-created")
  } catch (e: unknown) {
    const status = (e as { status?: number }).status
    if (status === 409) {
      await props.fetchPage()
      for (let i = 0; i < 20 && props.indexNoteStatus !== "present"; i++) {
        await nextTick()
      }
      if (props.indexNoteStatus === "present") {
        showSuccessToast(props.successToastAfterRace)
        return
      }
      showErrorToast(props.errorToastAfterRace)
    }
  } finally {
    savingIndexDraft.value = false
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
