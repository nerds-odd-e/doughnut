<template>
  <div
    v-if="understandingPoints.length > 0"
    class="mb-4 rounded-lg bg-accent p-4"
    data-test-id="understanding-checklist"
  >
    <div class="text-base">
      <div class="font-semibold mb-3 text-accent-content">
        Understanding Checklist:
      </div>
      <ul class="space-y-2">
        <li
          v-for="(point, index) in understandingPoints"
          :key="index"
          class="text-accent-content flex items-start gap-2"
        >
          <label
            class="flex items-start cursor-pointer gap-2 flex-1 min-w-0"
          >
            <input
              type="checkbox"
              :value="index"
              v-model="selectedPointIndices"
              class="daisy-checkbox daisy-checkbox-accent daisy-checkbox-sm mt-1 border-black dark:border-white hover:border-black hover:dark:border-white checked:border-black checked:dark:border-white border-2 shrink-0"
            />
            <span class="break-words">{{ point }}</span>
          </label>
          <div class="flex gap-1 shrink-0">
            <button
              class="daisy-btn daisy-btn-xs daisy-btn-ghost"
              @click="extractPointToSibling(point, index)"
              title="Promote to sibling note"
            >
              <Folders class="w-4 h-4" />
              Sibling
            </button>
          </div>
        </li>
      </ul>

      <div class="flex gap-2 mt-4">
        <button
          data-test-id="delete-understanding-points"
          :disabled="selectedPointIndices.length === 0"
          @click="deleteSelectedPoints"
          class="daisy-btn daisy-btn-error daisy-btn-sm !text-white"
        >
          Delete selected points
        </button>
      </div>
    </div>
  </div>
  <LoadingModal :show="isExtractingToSibling" message="AI is creating note..." />
  <LoadingModal :show="isDeletingPoints" message="AI is removing content..." />
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import { AiController } from "@generated/doughnut-backend-api/sdk.gen"

import { apiCallWithLoading } from "@/managedApi/clientSetup"
import usePopups from "../commons/Popups/usePopups"
import { Folders } from "@lucide/vue"
import LoadingModal from "../commons/LoadingModal.vue"
import { onMounted, ref } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const props = defineProps<{
  note: Note
}>()

const emit = defineEmits<{
  (e: "contentUpdated", newContent: string): void
}>()

const understandingPoints = ref<string[]>([])

const generateUnderstandingChecklist = async () => {
  try {
    const result = await apiCallWithLoading(() =>
      AiController.generateUnderstandingChecklist({
        path: { note: props.note.id },
      })
    )

    if (!result.error && result.data) {
      understandingPoints.value = result.data.points || []
    } else {
      understandingPoints.value = []
    }
  } catch (err) {
    console.error("Failed to generate understanding checklist:", err)
    understandingPoints.value = []
  }
}

onMounted(() => generateUnderstandingChecklist())

const { popups } = usePopups()
const storageAccessor = useStorageAccessor()

const selectedPointIndices = ref<number[]>([])
const isExtractingToSibling = ref(false)
const isDeletingPoints = ref(false)

const deleteSelectedPoints = async () => {
  if (selectedPointIndices.value.length === 0) {
    return
  }

  const confirmed = await popups.confirm(
    `Are you sure you want to delete ${selectedPointIndices.value.length} selected point(s)? The AI will remove related content from the note.`
  )

  if (!confirmed) {
    return
  }

  const selectedPoints = selectedPointIndices.value.map(
    (index) => understandingPoints.value[index]!
  )

  isDeletingPoints.value = true
  try {
    const { data, error } = await apiCallWithLoading(() =>
      AiController.removePointFromNote({
        path: { note: props.note.id },
        body: { points: selectedPoints },
      })
    )

    if (!error && data?.content !== undefined) {
      const storedApi = storageAccessor.value?.storedApi()
      if (storedApi) {
        await storedApi.updateTextField(
          props.note.id,
          "edit content",
          data.content
        )
      }
      emit("contentUpdated", data.content)
    }
  } finally {
    isDeletingPoints.value = false
  }
}

const extractPointToSibling = async (point: string, index: number) => {
  isExtractingToSibling.value = true
  try {
    const response = await apiCallWithLoading(() =>
      AiController.promotePointToSibling({
        path: { note: props.note.id },
        body: { points: [point] },
      })
    )

    if (response.error || !response.data) {
      await popups.alert("Failed to create note with AI")
      return
    }

    if (storageAccessor.value) {
      storageAccessor.value.refreshNoteRealm(response.data)
    }

    understandingPoints.value.splice(index, 1)
  } catch (err) {
    console.error("Failed to extract point to sibling:", err)
    await popups.alert(`Error: ${err}`)
  } finally {
    isExtractingToSibling.value = false
  }
}
</script>
