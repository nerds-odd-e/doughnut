<template>
  <div
    v-if="refinementSuggestions.length > 0"
    class="mb-4 rounded-lg bg-accent p-4"
    data-test-id="refinement-suggestions"
  >
    <div class="text-base">
      <div class="font-semibold mb-3 text-accent-content">
        Refinement suggestions:
      </div>
      <ul class="space-y-2">
        <li
          v-for="(suggestion, index) in refinementSuggestions"
          :key="index"
          class="text-accent-content flex items-start gap-2"
        >
          <label
            class="flex items-start cursor-pointer gap-2 flex-1 min-w-0"
          >
            <input
              type="checkbox"
              :value="index"
              v-model="selectedSuggestionIndices"
              class="daisy-checkbox daisy-checkbox-accent daisy-checkbox-sm mt-1 border-black dark:border-white hover:border-black hover:dark:border-white checked:border-black checked:dark:border-white border-2 shrink-0"
            />
            <span class="break-words">{{ suggestion }}</span>
          </label>
          <div class="flex gap-1 shrink-0">
            <button
              class="daisy-btn daisy-btn-xs daisy-btn-ghost"
              @click="extractNote(suggestion)"
              title="Extract to a new note"
            >
              <Folders class="w-4 h-4" />
              Extract note
            </button>
          </div>
        </li>
      </ul>

      <div class="flex gap-2 mt-4">
        <button
          data-test-id="remove-refinement-suggestions"
          :disabled="selectedSuggestionIndices.length === 0"
          @click="removeSelectedSuggestions"
          class="daisy-btn daisy-btn-error daisy-btn-sm !text-white"
        >
          Remove selected
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type {
  Note,
  NoteRefinementLayoutItem,
} from "@generated/doughnut-backend-api"
import { AiController } from "@generated/doughnut-backend-api/sdk.gen"

import {
  apiCallWithLoading,
  runWithBlockingApiLoading,
} from "@/managedApi/clientSetup"
import usePopups from "../commons/Popups/usePopups"
import { Folders } from "@lucide/vue"
import { onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const props = defineProps<{
  note: Note
}>()

const emit = defineEmits<{
  (e: "contentUpdated", newContent: string): void
}>()

const refinementSuggestions = ref<string[]>([])

const flattenLayoutItems = (items: NoteRefinementLayoutItem[]): string[] =>
  items.flatMap((item) => [
    item.text,
    ...flattenLayoutItems(item.children ?? []),
  ])

const loadRefinementSuggestions = async () => {
  try {
    const result = await apiCallWithLoading(() =>
      AiController.generateRefinementSuggestions({
        path: { note: props.note.id },
      })
    )

    refinementSuggestions.value =
      !result.error && result.data?.items
        ? flattenLayoutItems(result.data.items)
        : []
  } catch (err) {
    console.error("Failed to generate refinement suggestions:", err)
    refinementSuggestions.value = []
  }
}

onMounted(() => loadRefinementSuggestions())

const { popups } = usePopups()
const router = useRouter()
const storageAccessor = useStorageAccessor()

const selectedSuggestionIndices = ref<number[]>([])

const removeSelectedSuggestions = async () => {
  if (selectedSuggestionIndices.value.length === 0) {
    return
  }

  const confirmed = await popups.confirm(
    `Are you sure you want to remove ${selectedSuggestionIndices.value.length} selected suggestion(s)? The AI will remove related content from the note.`
  )

  if (!confirmed) {
    return
  }

  const selectedSuggestions = selectedSuggestionIndices.value.map(
    (index) => refinementSuggestions.value[index]!
  )

  await runWithBlockingApiLoading(async () => {
    const { data, error } = await apiCallWithLoading(() =>
      AiController.removeRefinementSuggestion({
        path: { note: props.note.id },
        body: { suggestions: selectedSuggestions },
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
  }, "AI is removing content...")
}

const extractNote = async (suggestion: string) => {
  try {
    await runWithBlockingApiLoading(async () => {
      const response = await apiCallWithLoading(() =>
        AiController.extractNote({
          path: { note: props.note.id },
          body: { suggestions: [suggestion] },
        })
      )

      if (response.error || !response.data) {
        await popups.alert("Failed to create note with AI")
        return
      }

      await storageAccessor.value
        .storedApi()
        .focusNoteRealm(router, response.data)
    }, "AI is creating note...")
  } catch (err) {
    console.error("Failed to extract note:", err)
    await popups.alert(`Error: ${err}`)
  }
}
</script>
