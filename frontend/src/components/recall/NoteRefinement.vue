<template>
  <div
    v-if="refinementLayoutItems.length > 0"
    class="mb-4 rounded-lg bg-accent p-4"
    data-test-id="refinement-layout"
  >
    <div class="text-base">
      <div class="font-semibold mb-3 text-accent-content">
        Note layout:
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
          data-test-id="extract-refinement-layout"
          :disabled="selectedItemIds.length === 0"
          @click="extractNote"
          class="daisy-btn daisy-btn-primary daisy-btn-sm"
          title="Extract selected to a new note"
        >
          <Folders class="w-4 h-4" />
          Extract
        </button>
        <button
          data-test-id="remove-refinement-layout"
          :disabled="selectedItemIds.length === 0"
          @click="removeSelectedLayoutItems"
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
  isFullySelected,
  isPartiallySelected,
  setItemSelection,
  clearSelection,
} = useRefinementLayoutSelection(refinementLayoutItems)

const loadRefinementLayout = async () => {
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
    console.error("Failed to generate note layout:", err)
    refinementLayoutItems.value = []
    clearSelection()
  }
}

onMounted(() => loadRefinementLayout())

const { popups } = usePopups()
const router = useRouter()
const storageAccessor = useStorageAccessor()

const removeSelectedLayoutItems = async () => {
  if (selectedItemIds.value.length === 0) {
    return
  }

  const confirmed = await popups.confirm(
    `Are you sure you want to remove ${selectedItemIds.value.length} selected layout point(s)? The AI will remove related content from the note.`
  )

  if (!confirmed) {
    return
  }

  await runWithBlockingApiLoading(async () => {
    const { data, error } = await apiCallWithLoading(() =>
      AiController.removeRefinementSuggestion({
        path: { note: props.note.id },
        body: {
          layout: { items: refinementLayoutItems.value },
          selectedItemIds: selectedItemIds.value,
        },
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

const extractNote = async () => {
  try {
    await runWithBlockingApiLoading(async () => {
      const response = await apiCallWithLoading(() =>
        AiController.extractNote({
          path: { note: props.note.id },
          body: {
            layout: { items: refinementLayoutItems.value },
            selectedItemIds: selectedItemIds.value,
          },
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
