<template>
  <div
    class="notebook-settings"
    data-testid="notebook-settings"
  >
    <section class="bg-base-100 border border-base-300 rounded-lg p-6 mb-6">
      <div class="flex flex-col gap-2">
        <TextInput
          scope-name="notebook"
          field="description"
          v-model="settingsBody.description"
          placeholder="Optional short plain-text message (shown on notebook cards)"
        />
        <p class="text-xs text-base-content/60 leading-snug mt-1">
          Plain text only, up to 500 characters. This is separate from notebook page body
          content.
        </p>
      </div>
      <button
        class="daisy-btn daisy-btn-primary daisy-btn-sm mt-4"
        type="button"
        data-testid="notebook-description-save"
        @click="saveDescription"
      >
        Save Description
      </button>
    </section>

    <section class="bg-base-100 border border-base-300 rounded-lg p-6 mb-6">
      <div class="flex flex-col gap-2">
        <CheckInput
          scope-name="notebook"
          field="skipMemoryTrackingEntirely"
          title="Skip Memory Tracking"
          :model-value="settingsBody.skipMemoryTrackingEntirely"
          :error-message="errors.skipMemoryTrackingEntirely"
          @update:model-value="onSkipMemoryTrackingChange"
        />
        <p class="text-xs text-base-content/60 leading-snug mt-1">
          When enabled, notes in this notebook are left out of the assimilation sequence, and others cannot subscribe from the Bazaar. Existing memory trackers still appear in recall.
        </p>
      </div>
    </section>

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
        <button
          class="daisy-btn daisy-btn-outline daisy-btn-sm"
          type="button"
          data-testid="notebook-settings-reset-git-history"
          @click="resetGitHistory()"
        >
          <div class="flex items-center gap-2">
            <History class="w-6 h-6" />
            <span>Reset Git history</span>
          </div>
        </button>
      </div>
    </section>

    <NotebookIndexingSection :notebook-id="notebook.id" />
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
} from "@generated/donut-backend-api"
import { NotebookController } from "@generated/donut-backend-api/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useToast } from "@/composables/useToast"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { GitMerge, Share2, History } from "@lucide/vue"
import NotebookMoveForm from "@/components/notebook/NotebookMoveForm.vue"
import CheckInput from "@/components/form/CheckInput.vue"
import TextInput from "@/components/form/TextInput.vue"
import NotebookAttachedBookSection from "@/components/notebook/NotebookAttachedBookSection.vue"
import NotebookIndexingSection from "@/components/notebook/NotebookIndexingSection.vue"

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

const resetGitHistory = async () => {
  if (
    await popups.confirm(
      `Reset Git history? The current history is permanently discarded and replaced by one commit of this notebook. Existing clones stop working and have to be cloned again.`
    )
  ) {
    const { error } = await apiCallWithLoading(() =>
      NotebookController.resetNotebookGitHistory({
        path: { notebook: props.notebook.id },
      })
    )
    if (!error) {
      showSuccessToast("Notebook Git history reset")
    }
  }
}

const persistSettings = async (successMessage: string) => {
  const { data: updatedNotebook, error } = await apiCallWithLoading(() =>
    NotebookController.updateNotebook({
      path: { notebook: props.notebook.id },
      body: props.settingsBody,
    })
  )
  if (!error) {
    emit("notebook-updated", updatedNotebook!)
    showSuccessToast(successMessage)
    return
  }
  const errorObj = toOpenApiError(error)
  errors.value = { ...errors.value, ...(errorObj.errors || {}) }
}

const saveDescription = async () => {
  await persistSettings("Notebook description updated")
}

const onSkipMemoryTrackingChange = async (checked: boolean) => {
  props.settingsBody.skipMemoryTrackingEntirely = checked
  await persistSettings("Memory tracking setting updated")
}
</script>
