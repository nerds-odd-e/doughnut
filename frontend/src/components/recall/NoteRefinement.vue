<template>
  <div
    v-if="understandingPoints.length > 0"
    class="daisy-mb-4 daisy-rounded-lg daisy-bg-accent daisy-p-4"
    data-test-id="understanding-checklist"
  >
    <div class="daisy-text-base">
      <div class="daisy-font-semibold daisy-mb-3 daisy-text-accent-content">
        Understanding Checklist:
      </div>
      <ul class="daisy-space-y-2">
        <li
          v-for="(point, index) in understandingPoints"
          :key="index"
          class="daisy-text-accent-content daisy-flex daisy-items-start daisy-gap-2"
        >
          <label
            class="daisy-flex daisy-items-start daisy-cursor-pointer daisy-gap-2 daisy-flex-1 daisy-min-w-0"
          >
            <input
              type="checkbox"
              :value="index"
              v-model="selectedPointIndices"
              class="daisy-checkbox daisy-checkbox-accent daisy-checkbox-sm daisy-mt-1 daisy-border-black dark:daisy-border-white hover:daisy-border-black hover:dark:daisy-border-white checked:daisy-border-black checked:dark:daisy-border-white daisy-border-2 daisy-shrink-0"
            />
            <span class="daisy-break-words">{{ point }}</span>
          </label>
          <div class="daisy-flex daisy-gap-1 daisy-shrink-0">
            <button
              class="daisy-btn daisy-btn-xs daisy-btn-ghost"
              @click="promotePointToChildNote(point, index)"
              title="Promote to child note"
            >
              <SvgAddChild class="w-4 h-4" />
              Child
            </button>
            <button
              v-if="note.parentId"
              class="daisy-btn daisy-btn-xs daisy-btn-ghost"
              @click="promotePointToSiblingNote(point, index)"
              title="Promote to sibling note"
            >
              <SvgAddSibling class="w-4 h-4" />
              Sibling
            </button>
          </div>
        </li>
      </ul>

      <div class="daisy-flex daisy-gap-2 daisy-mt-4">
        <button
          data-test-id="delete-understanding-points"
          :disabled="selectedPointIndices.length === 0"
          @click="deleteSelectedPoints"
          class="daisy-btn daisy-btn-error daisy-btn-sm !daisy-text-white"
        >
          Delete selected points
        </button>
        <button
          data-test-id="ignore-understanding-points"
          :disabled="selectedPointIndices.length === 0"
          @click="ignoreSelectedPoints"
          class="daisy-btn daisy-btn-warning daisy-btn-sm !daisy-text-white"
        >
          Ignore questions
        </button>
      </div>
    </div>
  </div>
  <LoadingModal :show="isPromotingPoint" message="AI is creating note..." />
  <LoadingModal :show="isDeletingPoints" message="AI is removing content..." />
</template>

<script setup lang="ts">
import type { Note, PromotePointRequestDto } from "@generated/backend"
import { AiController } from "@generated/backend/sdk.gen"

const PromotionType = {
  CHILD: "CHILD",
  SIBLING: "SIBLING",
} as const satisfies Record<string, PromotePointRequestDto["promotionType"]>

import { apiCallWithLoading } from "@/managedApi/clientSetup"
import usePopups from "../commons/Popups/usePopups"
import SvgAddChild from "../svgs/SvgAddChild.vue"
import SvgAddSibling from "../svgs/SvgAddSibling.vue"
import LoadingModal from "../commons/LoadingModal.vue"
import { ref, watch } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const props = defineProps<{
  note: Note
  currentNoteDetails: string
  refreshTrigger: number
}>()

const emit = defineEmits<{
  (e: "reloadNeeded"): void
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

watch(
  () => props.refreshTrigger,
  () => generateUnderstandingChecklist(),
  { immediate: true }
)

const { popups } = usePopups()
const storageAccessor = useStorageAccessor()

const selectedPointIndices = ref<number[]>([])
const isPromotingPoint = ref(false)
const isDeletingPoints = ref(false)

const deleteSelectedPoints = async () => {
  if (selectedPointIndices.value.length === 0) {
    return
  }

  const confirmed = await popups.confirm(
    `Are you sure you want to delete ${selectedPointIndices.value.length} selected point(s)? The AI will remove related content from the note details.`
  )

  if (!confirmed) {
    return
  }

  const selectedPoints = selectedPointIndices.value.map(
    (index) => understandingPoints.value[index]!
  )

  isDeletingPoints.value = true
  try {
    const { error } = await apiCallWithLoading(() =>
      AiController.removePointFromNote({
        path: { note: props.note.id },
        body: { points: selectedPoints },
      })
    )

    if (!error) {
      emit("reloadNeeded")
    }
  } finally {
    isDeletingPoints.value = false
  }
}

const ignoreSelectedPoints = async () => {
  if (selectedPointIndices.value.length === 0) {
    return
  }

  const confirmed = await popups.confirm(
    `Ignore ${selectedPointIndices.value.length} selected point(s) so their questions won't appear when recalling?`
  )

  if (!confirmed) {
    return
  }

  const selectedPoints = selectedPointIndices.value.map(
    (index) => understandingPoints.value[index]!
  )

  await apiCallWithLoading(() =>
    AiController.ignorePoints({
      path: { note: props.note.id },
      body: { points: selectedPoints },
    })
  )
}

const promotePoint = async (
  point: string,
  index: number,
  promotionType: PromotePointRequestDto["promotionType"]
) => {
  isPromotingPoint.value = true
  try {
    const { data: result, error } = await apiCallWithLoading(() =>
      AiController.promotePoint({
        path: { note: props.note.id },
        body: { point, promotionType },
      })
    )

    if (error || !result || !result.createdNote || !result.updatedParentNote) {
      await popups.alert("Failed to create note with AI")
      return
    }

    const createdNote = result.createdNote
    const updatedParentNote = result.updatedParentNote

    if (storageAccessor.value) {
      storageAccessor.value.refreshNoteRealm(createdNote)
      storageAccessor.value.refreshNoteRealm(updatedParentNote)
    }

    understandingPoints.value.splice(index, 1)
  } catch (err) {
    console.error("Failed to promote point:", err)
    await popups.alert(`Error: ${err}`)
  } finally {
    isPromotingPoint.value = false
  }
}

const promotePointToChildNote = (point: string, index: number) => {
  promotePoint(point, index, PromotionType.CHILD)
}

const promotePointToSiblingNote = (point: string, index: number) => {
  promotePoint(point, index, PromotionType.SIBLING)
}
</script>
