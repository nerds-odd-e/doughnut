<template>
  <div
    class="notebook-workspace-settings"
    data-testid="notebook-workspace-settings"
  >
    <NotebookAttachedBookSection :notebook-id="notebook.id" />

    <section class="bg-base-100 border border-base-300 rounded-lg p-6 mb-6">
      <div class="mb-5">
        <h4 class="text-lg font-semibold mb-2 text-base-content">
          Notebook Management
        </h4>
        <p class="text-sm text-base-content/70 leading-normal">
          Manage your notebook's organization and sharing settings.
        </p>
      </div>
      <div class="flex flex-wrap gap-2">
        <PopButton
          title="Move to ..."
          v-if="user?.externalIdentifier === notebook.creatorId"
          btnClass="daisy-btn daisy-btn-outline daisy-btn-sm"
        >
          <template #button_face>
            <div class="flex items-center gap-2">
              <GitMerge class="w-6 h-6" />
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
          <div class="flex items-center gap-2">
            <Share2 class="w-6 h-6" />
            <span>Share notebook to bazaar</span>
          </div>
        </button>
      </div>
    </section>

    <section class="bg-base-100 border border-base-300 rounded-lg p-6 mb-6">
      <div class="mb-5">
        <h4 class="text-lg font-semibold mb-2 text-base-content">
          Notebook Settings
        </h4>
        <p class="text-sm text-base-content/70 leading-normal">
          Configure memory tracking and an optional short plain-text message
          for this notebook.
        </p>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div class="flex flex-col gap-2 col-span-full">
          <TextArea
            scope-name="notebook"
            field="description"
            v-model="settingsBody.description"
            :rows="3"
            placeholder="Optional short plain-text message (shown on notebook cards)"
          />
          <p class="text-xs text-base-content/60 leading-snug mt-1">
            Plain text only, up to 500 characters. This is separate from notebook page body
            content.
          </p>
        </div>
        <div class="flex flex-col gap-2">
          <CheckInput
            scope-name="notebook"
            field="skipMemoryTrackingEntirely"
            title="Skip Memory Tracking"
            v-model="settingsBody.skipMemoryTrackingEntirely"
            :error-message="errors.skipMemoryTrackingEntirely"
          />
          <p class="text-xs text-base-content/60 leading-snug mt-1">
            When enabled, notes in this notebook will not be included in memory tracking and recall sessions.
          </p>
        </div>
      </div>
      <button class="daisy-btn daisy-btn-primary daisy-btn-sm mt-4" @click="processForm">
        Update Settings
      </button>
    </section>

    <section class="bg-base-100 border border-base-300 rounded-lg p-6 mb-6">
      <div class="mb-5">
        <h4 class="text-lg font-semibold mb-2 text-base-content">
          Notebook Indexing
        </h4>
        <p class="text-sm text-base-content/70 leading-normal">
          Manage the search index for this notebook. Reset to rebuild from scratch, or update to index new content.
        </p>
      </div>
      <div class="flex flex-wrap gap-2">
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
import { ref } from "vue"
import { useRouter } from "vue-router"
import type {
  Notebook,
  NotebookUpdateRequest,
  User,
} from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useToast } from "@/composables/useToast"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { GitMerge, Share2 } from "@lucide/vue"
import NotebookMoveForm from "@/components/notebook/NotebookMoveForm.vue"
import CheckInput from "@/components/form/CheckInput.vue"
import TextArea from "@/components/form/TextArea.vue"
import NotebookAttachedBookSection from "@/components/notebook/NotebookAttachedBookSection.vue"

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
  user: { type: Object as PropType<User>, required: false },
  settingsBody: {
    type: Object as PropType<NotebookUpdateRequest>,
    required: true,
  },
})

const emit = defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
}>()

const { showSuccessToast } = useToast()
const router = useRouter()
const { popups } = usePopups()

const errors = ref({
  skipMemoryTrackingEntirely: undefined as string | undefined,
})
const isIndexing = ref(false)

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

const processForm = async () => {
  const { data: updatedNotebook, error } = await apiCallWithLoading(() =>
    NotebookController.updateNotebook({
      path: { notebook: props.notebook.id },
      body: props.settingsBody,
    })
  )
  if (!error) {
    emit("notebook-updated", updatedNotebook!)
    showSuccessToast("Notebook settings updated successfully")
  } else {
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
  }
  isIndexing.value = false
}
</script>
