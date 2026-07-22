<template>
  <div
    class="notebook-workspace-health bg-base-100 border border-base-300 rounded-lg p-6"
    data-testid="notebook-workspace-health"
  >
    <div class="flex flex-wrap items-center gap-2 mb-6">
      <button
        type="button"
        class="daisy-btn daisy-btn-primary daisy-btn-sm"
        data-testid="notebook-health-run"
        @click="runLint"
      >
        Run lint
      </button>
      <div data-testid="notebook-health-remove-empty-folders">
        <CheckInput
          scope-name="notebook-health"
          field="removeEmptyFolders"
          title="Remove empty folders"
          v-model="removeEmptyFolders"
        />
      </div>
      <button
        type="button"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        data-testid="notebook-health-save-defaults"
        @click="saveAsDefaults"
      >
        Save as defaults
      </button>
    </div>

    <p
      v-if="report === null"
      class="text-sm text-base-content/70"
      data-testid="notebook-health-idle"
    >
      Run lint to check this notebook for empty folders, readme-only folders,
      and dead wiki links.
    </p>

    <NotebookHealthFindings
      v-else
      :groups="report.groups ?? []"
    />
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, ref, type Ref } from "vue"
import type {
  NotebookHealthLintReport,
  User,
} from "@generated/doughnut-backend-api"
import {
  NotebookHealthController,
  UserController,
} from "@generated/doughnut-backend-api/sdk.gen"
import CheckInput from "@/components/form/CheckInput.vue"
import NotebookHealthFindings from "@/components/notebook/NotebookHealthFindings.vue"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const props = defineProps<{
  notebookId: number
}>()

const currentUser = inject<Ref<User | undefined>>("currentUser")
const report = ref<NotebookHealthLintReport | null>(null)
const removeEmptyFolders = ref(false)

onMounted(() => {
  removeEmptyFolders.value =
    currentUser?.value?.healthRemoveEmptyFoldersDefault ?? false
})

async function runLint() {
  const { data, error } = await apiCallWithLoading(() =>
    NotebookHealthController.lint({
      path: { notebook: props.notebookId },
    })
  )
  if (!error) {
    report.value = data!
  }
}

async function saveAsDefaults() {
  const user = currentUser?.value
  if (!user) return
  const { data, error } = await apiCallWithLoading(() =>
    UserController.updateUser({
      path: { user: user.id },
      body: {
        name: user.name,
        dailyAssimilationCount: user.dailyAssimilationCount,
        spaceIntervals: user.spaceIntervals,
        healthRemoveEmptyFoldersDefault: removeEmptyFolders.value,
      },
    })
  )
  if (!error && currentUser) {
    currentUser.value = data!
  }
}
</script>
