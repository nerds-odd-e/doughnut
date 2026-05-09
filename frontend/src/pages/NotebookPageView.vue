<template>
  <div class="daisy-container daisy-mx-auto daisy-py-4 daisy-max-w-6xl">
    <div class="notebook-page-summary daisy-mb-6" data-testid="notebook-page-summary">
      <NotebookPageNameEditor
        :notebook-id="notebook.id"
        :name="notebook.name ?? ''"
        :settings-body="formData"
        @notebook-updated="(n) => emit('notebook-updated', n)"
      />
      <p v-if="notebook.description" class="notebook-page-summary-description">
        {{ notebook.description }}
      </p>
    </div>

    <div
      v-if="indexNoteStatus === 'pending'"
      class="notebook-page-index-body daisy-mb-6 daisy-flex daisy-items-center daisy-gap-2"
      data-testid="notebook-index-loading"
    >
      <span class="daisy-loading daisy-loading-spinner daisy-loading-sm" />
      <span class="daisy-text-sm daisy-text-base-content/70">Loading index…</span>
    </div>

    <div
      v-else-if="indexNoteStatus === 'present' && indexRealm && indexNoteId != null"
      class="notebook-page-index-body daisy-mb-6"
      data-testid="notebook-index-body"
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
        data-testid="notebook-index-editable-content"
        @dead-link-click="pendingDeadLinkTitle = $event"
      />
      <NoteDeadLinkCreateModal
        v-model="pendingDeadLinkTitle"
        :notebook-id="notebook.id"
        :note-realm="indexRealm"
        :source-note-id="indexRealm.id"
      />
    </div>

    <div
      v-else-if="indexNoteStatus === 'absent'"
      class="notebook-page-index-body daisy-mb-6"
      data-testid="notebook-index-body"
    >
      <h2 class="daisy-text-lg daisy-font-semibold daisy-text-base-content daisy-mb-2">
        Index
      </h2>
      <p class="daisy-text-sm daisy-text-base-content/70 daisy-mb-3">
        No index note yet. Edit below and save to create a root note titled
        <span class="daisy-font-mono">index</span>.
      </p>
      <div data-testid="notebook-index-draft-editor">
        <RichMarkdownEditor
          v-model="indexDraftMarkdown"
          :multiple-line="true"
          scope-name="notebook-index"
          field="content"
          :readonly="false"
          :wiki-titles="[]"
        />
      </div>
      <button
        type="button"
        class="daisy-btn daisy-btn-primary daisy-btn-sm daisy-mt-3"
        data-testid="notebook-index-create-save"
        :disabled="savingIndexDraft"
        @click="saveIndexDraft"
      >
        {{ savingIndexDraft ? "Saving…" : "Save index" }}
      </button>
    </div>

    <NotebookAttachedBookSection :notebook-id="notebook.id" />

    <!-- Notebook Management Section -->
    <section class="settings-section daisy-mb-6">
      <div class="section-header">
        <h4 class="section-title">Notebook Management</h4>
        <p class="section-description">
          Manage your notebook's organization and sharing settings.
        </p>
      </div>
      <div class="daisy-flex daisy-flex-wrap daisy-gap-2">
        <PopButton
          title="Move to ..."
          v-if="user?.externalIdentifier === notebook.creatorId"
          btnClass="daisy-btn daisy-btn-outline daisy-btn-sm"
        >
          <template #button_face>
            <div class="daisy-flex daisy-items-center daisy-gap-2">
              <GitMerge class="daisy-w-6 daisy-h-6" />
              <span>Move to ...</span>
            </div>
          </template>
          <NotebookMoveForm v-bind="{ notebook }" />
        </PopButton>
        <button
          class="daisy-btn daisy-btn-outline daisy-btn-sm"
          @click="shareNotebook()"
          title="Share notebook to bazaar"
        >
          <div class="daisy-flex daisy-items-center daisy-gap-2">
            <Share2 class="daisy-w-6 daisy-h-6" />
            <span>Share notebook to bazaar</span>
          </div>
        </button>
      </div>
    </section>
    
    <!-- Notebook Settings Section -->
    <section class="settings-section daisy-mb-6">
      <div class="section-header">
        <h4 class="section-title">Notebook Settings</h4>
        <p class="section-description">
          Configure memory tracking and an optional short plain-text message
          for this notebook.
        </p>
      </div>
      <div class="settings-grid">
        <div class="settings-item settings-item-full-width">
          <TextArea
            scope-name="notebook"
            field="description"
            v-model="formData.description"
            :rows="3"
            placeholder="Optional short plain-text message (shown on notebook cards)"
          />
          <p class="field-hint">
            Plain text only, up to 500 characters. This is separate from notebook page body
            content.
          </p>
        </div>
        <div class="settings-item">
          <CheckInput
            scope-name="notebook"
            field="skipMemoryTrackingEntirely"
            title="Skip Memory Tracking"
            v-model="formData.skipMemoryTrackingEntirely"
            :error-message="errors.skipMemoryTrackingEntirely"
          />
          <p class="field-hint">
            When enabled, notes in this notebook will not be included in memory tracking and recall sessions.
          </p>
        </div>
      </div>
      <button class="daisy-btn daisy-btn-primary daisy-btn-sm daisy-mt-4" @click="processForm">
        Update Settings
      </button>
    </section>

    <section class="settings-section daisy-mb-6">
      <div class="section-header">
        <h4 class="section-title">Assistant Management</h4>
        <p class="section-description">
          Configure AI assistant settings and instructions for this notebook.
        </p>
      </div>
      <NotebookAssistantManagementForm 
        :notebook="notebook" 
        :additional-instructions="additionalInstructions"
      />
    </section>

    <!-- Notebook Indexing Section -->
    <section class="settings-section daisy-mb-6">
      <div class="section-header">
        <h4 class="section-title">Notebook Indexing</h4>
        <p class="section-description">
          Manage the search index for this notebook. Reset to rebuild from scratch, or update to index new content.
        </p>
      </div>
      <div class="daisy-flex daisy-flex-wrap daisy-gap-2">
        <button
          class="daisy-btn daisy-btn-outline daisy-btn-sm"
          @click="reindexNotebook"
          :disabled="isIndexing"
        >
          <span v-if="isIndexing">Working...</span>
          <span v-else>Reset notebook index</span>
        </button>
        <button
          class="daisy-btn daisy-btn-outline daisy-btn-sm"
          @click="updateIndexNotebook"
          :disabled="isIndexing"
        >
          <span v-if="isIndexing">Working...</span>
          <span v-else>Update index</span>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref, watch, computed, nextTick } from "vue"
import { useRouter } from "vue-router"
import type {
  Notebook,
  User,
  NotebookAiAssistant,
} from "@generated/doughnut-backend-api"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useToast } from "@/composables/useToast"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { GitMerge, Share2 } from "lucide-vue-next"
import NotebookMoveForm from "@/components/notebook/NotebookMoveForm.vue"
import CheckInput from "@/components/form/CheckInput.vue"
import TextArea from "@/components/form/TextArea.vue"
import NotebookAttachedBookSection from "@/components/notebook/NotebookAttachedBookSection.vue"
import NotebookAssistantManagementForm from "@/components/notebook/NotebookAssistantManagementForm.vue"
import NotebookPageNameEditor from "@/components/notebook/NotebookPageNameEditor.vue"
import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue"
import NoteEditableContent from "@/components/notes/core/NoteEditableContent.vue"
import NoteDeadLinkCreateModal from "@/components/notes/NoteDeadLinkCreateModal.vue"

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
  user: { type: Object as PropType<User>, required: false },
  fetchNotebookPage: {
    type: Function as PropType<() => Promise<void>>,
    required: true,
  },
  indexNoteStatus: {
    type: String as PropType<"pending" | "present" | "absent">,
    default: "absent",
  },
  indexNoteId: { type: Number, required: false },
})

const storageAccessor = useStorageAccessor()

const indexRealm = computed(() => {
  if (props.indexNoteStatus !== "present" || props.indexNoteId == null) {
    return undefined
  }
  return storageAccessor.value.refOfNoteRealm(props.indexNoteId).value
})

const indexDisplayTitle = computed(() => {
  const t = indexRealm.value?.note.noteTopology.title.trim()
  return t && t.length > 0 ? t : "Index"
})

const aiAssistant = ref<NotebookAiAssistant | undefined>(undefined)

const additionalInstructions = computed(
  () => aiAssistant.value?.additionalInstructionsToAi || ""
)

const fetchAiAssistant = async () => {
  const { data: assistant, error } = await apiCallWithLoading(() =>
    NotebookController.getAiAssistant({
      path: { notebook: props.notebook.id },
    })
  )
  if (!error) {
    aiAssistant.value = assistant!
  }
}

watch(
  () => props.notebook.id,
  () => {
    fetchAiAssistant()
  },
  { immediate: true }
)

const emit = defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
  (e: "index-note-created"): void
}>()

const { showSuccessToast, showErrorToast } = useToast()
const router = useRouter()
const { popups } = usePopups()

const indexDraftMarkdown = ref("")
const savingIndexDraft = ref(false)
const pendingDeadLinkTitle = ref<string | null>(null)

watch(
  () => [props.notebook.id, props.indexNoteStatus] as const,
  ([, status]) => {
    if (status === "absent") {
      indexDraftMarkdown.value = ""
    }
  }
)

const saveIndexDraft = async () => {
  savingIndexDraft.value = true
  try {
    await storageAccessor.value.storedApi().createRootNoteAtNotebook(
      router,
      props.notebook.id,
      {
        newTitle: "index",
        content: indexDraftMarkdown.value,
      },
      { skipRouterReplace: true }
    )
    showSuccessToast("Notebook index saved")
    emit("index-note-created")
  } catch (e: unknown) {
    const status = (e as { status?: number }).status
    if (status === 409) {
      await props.fetchNotebookPage()
      for (let i = 0; i < 20 && props.indexNoteStatus !== "present"; i++) {
        await nextTick()
      }
      if (props.indexNoteStatus === "present") {
        showSuccessToast("Notebook index is now available")
        return
      }
      showErrorToast(
        "Could not create index: a conflicting note may exist. Refresh the page and try again."
      )
    }
  } finally {
    savingIndexDraft.value = false
  }
}

const shareNotebook = async () => {
  if (await popups.confirm(`Confirm to share?`)) {
    const { error } = await apiCallWithLoading(() =>
      NotebookController.shareNotebook({
        path: { notebook: props.notebook.id },
      })
    )
    if (!error) {
      await router.push({ name: "notebooks" })
    }
  }
}

// Form data
const { skipMemoryTrackingEntirely } = props.notebook.notebookSettings

const formData = ref({
  skipMemoryTrackingEntirely,
  description: props.notebook.description ?? "",
})

watch(
  () => props.notebook,
  (nb) => {
    formData.value = {
      skipMemoryTrackingEntirely:
        nb.notebookSettings.skipMemoryTrackingEntirely ?? false,
      description: nb.description ?? "",
    }
  },
  { deep: true }
)

const errors = ref({
  skipMemoryTrackingEntirely: undefined as string | undefined,
})

// Indexing state
const isIndexing = ref(false)

const processForm = async () => {
  const { data: updatedNotebook, error } = await apiCallWithLoading(() =>
    NotebookController.updateNotebook({
      path: { notebook: props.notebook.id },
      body: formData.value,
    })
  )
  if (!error) {
    emit("notebook-updated", updatedNotebook!)
    showSuccessToast("Notebook settings updated successfully")
  } else {
    // Error is handled by global interceptor (toast notification)
    // Extract field-level errors if available (for 400 validation errors)
    const errorObj = toOpenApiError(error)
    errors.value = { ...errors.value, ...(errorObj.errors || {}) }
  }
}

const reindexNotebook = async () => {
  isIndexing.value = true
  const { error } = await apiCallWithLoading(() =>
    NotebookController.resetNotebookIndex({
      path: { notebook: props.notebook.id },
    })
  )
  if (!error) {
    showSuccessToast("Notebook index reset successfully")
  } else {
    // Error is handled by global interceptor (toast notification)
  }
  isIndexing.value = false
}

const updateIndexNotebook = async () => {
  isIndexing.value = true
  const { error } = await apiCallWithLoading(() =>
    NotebookController.updateNotebookIndex({
      path: { notebook: props.notebook.id },
    })
  )
  if (!error) {
    showSuccessToast("Notebook index updated successfully")
  } else {
    // Error is handled by global interceptor (toast notification)
  }
  isIndexing.value = false
}
</script>

<style scoped>
.notebook-page-summary {
  background: oklch(var(--b2) / 0.8);
  border-radius: 8px;
  margin-bottom: 1.5rem;
  padding: 1.5rem;
}

.notebook-page-summary-description {
  color: oklch(var(--bc) / 0.6);
  line-height: 1.6;
  margin-top: 0.5rem;
}

.notebook-page-index-body {
  background: oklch(var(--b2) / 0.8);
  border-radius: 8px;
  padding: 1rem 1.25rem;
}

.settings-section {
  background: oklch(var(--b1));
  border: 1px solid oklch(var(--b3));
  border-radius: 8px;
  padding: 1.5rem;
}

.section-header {
  margin-bottom: 1.25rem;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: oklch(var(--bc));
}

.section-description {
  font-size: 0.875rem;
  color: oklch(var(--bc) / 0.7);
  line-height: 1.5;
}

.settings-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
}

@media (min-width: 768px) {
  .settings-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.settings-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.settings-item-full-width {
  grid-column: 1 / -1;
}

.field-hint {
  font-size: 0.75rem;
  color: oklch(var(--bc) / 0.6);
  line-height: 1.4;
  margin-top: 0.25rem;
}
</style>

