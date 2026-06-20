<template>
  <div
    v-if="refinementLayoutItems.length > 0"
    class="mb-4 rounded-lg bg-accent p-4"
    data-test-id="refinement-suggestions"
  >
    <div class="text-base">
      <div class="font-semibold mb-3 text-accent-content">
        Refinement suggestions:
      </div>
      <ul class="space-y-2">
        <li
          v-for="item in refinementLayoutItems"
          :key="item.id"
          :data-test-id="`refinement-layout-item-${item.id}`"
          data-layout-level="1"
          class="text-accent-content"
        >
          <RefinementLayoutItemRow
            :item="item"
            :fully-selected="isFullySelected(item)"
            :partially-selected="isPartiallySelected(item)"
            @selection-change="setItemSelection(item, $event)"
          />

          <ul v-if="item.children.length > 0" class="ml-6 mt-2 space-y-2">
            <li
              v-for="child in item.children"
              :key="child.id"
              :data-test-id="`refinement-layout-item-${child.id}`"
              data-layout-level="2"
            >
              <RefinementLayoutItemRow
                :item="child"
                :fully-selected="isFullySelected(child)"
                :partially-selected="isPartiallySelected(child)"
                @selection-change="setItemSelection(child, $event)"
              />
            </li>
          </ul>
        </li>
      </ul>

      <div class="flex gap-2 mt-4">
        <button
          data-test-id="extract-refinement-suggestions"
          :disabled="selectedItemIds.length === 0"
          @click="extractSelectedNote"
          class="daisy-btn daisy-btn-primary daisy-btn-sm"
          title="Extract selected to a new note"
        >
          <Folders class="w-4 h-4" />
          Extract
        </button>
        <button
          data-test-id="remove-refinement-suggestions"
          :disabled="selectedItemIds.length === 0"
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
import { useRefinementLayoutSelection } from "@/composables/useRefinementLayoutSelection"
import usePopups from "../commons/Popups/usePopups"
import RefinementLayoutItemRow from "./RefinementLayoutItemRow.vue"
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

const refinementLayoutItems = ref<NoteRefinementLayoutItem[]>([])

const {
  selectedItemIds,
  layoutItemsById,
  isFullySelected,
  isPartiallySelected,
  setItemSelection,
  selectedTexts,
  clearSelection,
} = useRefinementLayoutSelection(refinementLayoutItems)

const loadRefinementSuggestions = async () => {
  try {
    const result = await apiCallWithLoading(() =>
      AiController.generateRefinementSuggestions({
        path: { note: props.note.id },
      })
    )

    refinementLayoutItems.value =
      !result.error && result.data?.items ? result.data.items : []
    clearSelection()
  } catch (err) {
    console.error("Failed to generate refinement suggestions:", err)
    refinementLayoutItems.value = []
    clearSelection()
  }
}

onMounted(() => loadRefinementSuggestions())

const { popups } = usePopups()
const router = useRouter()
const storageAccessor = useStorageAccessor()

const removeSelectedSuggestions = async () => {
  if (selectedItemIds.value.length === 0) {
    return
  }

  const confirmed = await popups.confirm(
    `Are you sure you want to remove ${selectedItemIds.value.length} selected suggestion(s)? The AI will remove related content from the note.`
  )

  if (!confirmed) {
    return
  }

  await runWithBlockingApiLoading(async () => {
    const { data, error } = await apiCallWithLoading(() =>
      AiController.removeRefinementSuggestion({
        path: { note: props.note.id },
        body: { suggestions: selectedTexts.value },
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

const extractSelectedNote = async () => {
  if (selectedItemIds.value.length === 0) {
    return
  }

  const selectedItems = selectedItemIds.value
    .map((id) => layoutItemsById.value.get(id))
    .filter((item): item is NoteRefinementLayoutItem => item !== undefined)

  if (selectedItems.length !== 1) {
    await popups.alert(
      "Please select one item to extract. Extracting multiple selected items is coming next."
    )
    return
  }

  const selectedItem = selectedItems[0]
  if (!selectedItem) {
    return
  }

  await extractNote(selectedItem.text)
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
