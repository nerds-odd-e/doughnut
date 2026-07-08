<template>
  <form
    id="wikidata-association-form"
    @submit.prevent="handleSave"
  >
    <div class="mb-4">
      <TextInput
        scope-name="wikidataID"
        field="wikidataID"
        :model-value="localWikidataId"
        @update:model-value="handleInputChange"
        :error-message="errorMessageComputed"
        :disabled="props.disabled"
        placeholder="example: `Q1234`"
      >
        <template #input_append>
          <button
            type="button"
            class="daisy-btn rounded-l-none"
            :class="[
              isLoadingUrl ? 'daisy-btn-disabled' : 'daisy-btn-primary',
            ]"
            title="open link"
            @click="handleOpenLink"
            :disabled="isLoadingUrl || !hasValidWikidataId"
            v-show="hasValidWikidataId"
          >
            <ExternalLink class="w-6 h-6" />
          </button>
        </template>
      </TextInput>
    </div>
  </form>
  <div v-if="loading" class="text-center p-4">
    Searching...
  </div>
  <div
    v-else-if="searchResults && searchResults.length === 0 && hasSearched"
    class="text-center p-4"
  >
    <p>No Wikidata entries found for '{{ searchKeyRef }}'</p>
  </div>
  <div v-else-if="searchResults && searchResults.length > 0 && !showTitleOptions">
    <div
      data-testid="wikidata-search-results"
      class="border border-base-300 rounded-lg bg-base-100 w-full"
      style="max-height: 300px; overflow-y: auto;"
    >
      <div
        v-for="suggestion in searchResults"
        :key="suggestion.id"
        data-testid="wikidata-search-result-item"
        :data-wikidata-id="suggestion.id"
        @click="onSelectSearchResultItem(suggestion.id)"
        :class="[
          'px-4 py-2 border-b border-base-300',
          props.disabled
            ? 'cursor-not-allowed opacity-50'
            : 'cursor-pointer hover:bg-base-200',
          selectedOption === suggestion.id ? 'bg-primary text-primary-content' : '',
        ]"
      >
        {{ suggestion.label }} - {{ suggestion.description }}
      </div>
    </div>
  </div>
  <div v-else-if="showTitleOptions" class="p-4">
    <label class="daisy-label">
      <span class="daisy-label-text"
        >Suggested Title: {{ selectedItem?.label }}</span
      >
    </label>
    <fieldset :disabled="props.disabled">
      <RadioButtons
        v-model="titleAction"
        scope-name="wikidataTitleAction"
        :options="[
          { value: 'Replace', label: 'Replace title' },
          { value: 'Append', label: 'Add as alias' },
        ]"
        @update:model-value="handleTitleAction"
      />
    </fieldset>
  </div>
  <div class="mt-4 flex gap-2">
    <button
      v-if="hasSaveButton"
      type="submit"
      form="wikidata-association-form"
      class="daisy-btn daisy-btn-primary"
      :disabled="!canSave || props.disabled"
    >
      Save
    </button>
    <button
      class="daisy-btn daisy-btn-secondary"
      @click="handleClose"
      :disabled="props.disabled"
    >
      Close
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick, computed } from "vue"
import type { WikidataSearchEntity } from "@generated/doughnut-backend-api"
import { WikidataController } from "@generated/doughnut-backend-api/sdk.gen"
import {} from "@/managedApi/clientSetup"
import RadioButtons from "../form/RadioButtons.vue"
import TextInput from "../form/TextInput.vue"
import { openWikidataEntityBrowseUrlInNonBlockingPopup } from "@/utils/wikidataEntityBrowseUrl"
import { ExternalLink } from "@lucide/vue"
import { useStableModalTop } from "@/composables/modalTopAnchor"

useStableModalTop()

const props = defineProps<{
  searchKey: string
  modelValue?: string
  errorMessage?: string
  showSaveButton?: boolean
  disabled?: boolean
  canSaveEmptyToClear?: boolean
  savedValue?: string
}>()

const emit = defineEmits<{
  close: []
  selected: [entity: WikidataSearchEntity, titleAction?: "replace" | "append"]
  "update:modelValue": [value: string]
  save: [wikidataId: string]
}>()

const hasSaveButton = computed(() => props.showSaveButton || false)

const searchKeyRef = computed(() => props.searchKey)

const errorMessageComputed = computed(() => props.errorMessage)

const localWikidataId = ref(props.modelValue || "")
const loading = ref(false)
const searchResults = ref<WikidataSearchEntity[]>([])
const selectedOption = ref("")
const selectedItem = ref<WikidataSearchEntity | null>(null)
const showTitleOptions = ref(false)
const titleAction = ref<"Replace" | "Append" | "">("")
const hasSearched = ref(false)
const isLoadingUrl = ref(false)

const hasValidWikidataId = computed(
  () => localWikidataId.value && localWikidataId.value.trim() !== ""
)

const hasValueChanged = computed(() => {
  const current = localWikidataId.value.trim()
  const saved = (props.savedValue ?? "").trim()
  return current !== saved
})

const canSave = computed(
  () =>
    hasValueChanged.value &&
    (hasValidWikidataId.value ||
      (!!props.canSaveEmptyToClear && localWikidataId.value.trim() === ""))
)

const fetchSearchResults = async () => {
  loading.value = true
  hasSearched.value = true
  try {
    const { data: results, error } = await WikidataController.searchWikidata({
      query: { search: searchKeyRef.value },
    })
    if (!error) {
      searchResults.value = results!
    }
  } finally {
    loading.value = false
  }
}

const compareTitles = (
  current: string,
  wikidata: string
): "match" | "different" => {
  const currentUpper = current.toUpperCase()
  const wikidataUpper = wikidata.toUpperCase()
  return currentUpper === wikidataUpper ? "match" : "different"
}

const selectSearchResult = (wikidataId: string) => {
  const selected = searchResults.value.find((obj) => obj.id === wikidataId)
  if (!selected || !selected.id) return null

  selectedItem.value = selected
  localWikidataId.value = selected.id

  const comparison = compareTitles(searchKeyRef.value, selected.label)

  if (comparison === "match") {
    showTitleOptions.value = false
    return { entity: selected, needsTitleAction: false as const }
  } else {
    showTitleOptions.value = true
    return { entity: selected, needsTitleAction: true as const }
  }
}

const getTitleAction = (): "replace" | "append" | undefined => {
  if (titleAction.value === "Replace") return "replace"
  if (titleAction.value === "Append") return "append"
  return undefined
}

const setWikidataId = (value: string) => {
  localWikidataId.value = value
}

const showTitleOptionsForEntity = (entity: WikidataSearchEntity) => {
  selectedItem.value = entity
  localWikidataId.value = entity.id || ""

  const comparison = compareTitles(searchKeyRef.value, entity.label)

  if (comparison === "match") {
    showTitleOptions.value = false
    return false
  } else {
    showTitleOptions.value = true
    return true
  }
}

defineExpose({
  showTitleOptionsForEntity,
  get showTitleOptions() {
    return showTitleOptions.value
  },
})

const handleOpenLink = () => {
  if (!hasValidWikidataId.value) return
  isLoadingUrl.value = true
  openWikidataEntityBrowseUrlInNonBlockingPopup(localWikidataId.value).finally(
    () => {
      isLoadingUrl.value = false
    }
  )
}

const handleInputChange = (value: string) => {
  setWikidataId(value)
  emit("update:modelValue", value)
}

const onSelectSearchResult = async () => {
  const result = selectSearchResult(selectedOption.value)
  if (!result || !result.entity.id) return

  emit("update:modelValue", result.entity.id)
  if (!result.needsTitleAction && !props.showSaveButton) {
    emit("selected", result.entity)
  }
}

const onSelectSearchResultItem = async (wikidataId: string | undefined) => {
  if (props.disabled || !wikidataId) return
  selectedOption.value = wikidataId
  await onSelectSearchResult()
}

const handleTitleAction = async () => {
  if (!selectedItem.value) return
  const action = getTitleAction()
  if (!action) return

  emit("selected", selectedItem.value, action)
}

const handleClose = () => {
  emit("close")
}

const handleSave = async () => {
  if (!canSave.value) return

  if (hasSaveButton.value) {
    if (showTitleOptions.value && selectedItem.value && titleAction.value) {
      const action = getTitleAction()
      emit("selected", selectedItem.value, action)
    } else {
      emit("save", localWikidataId.value.trim())
    }
  }
}

watch(
  () => props.modelValue,
  (newValue) => {
    if (newValue !== undefined) {
      setWikidataId(newValue)
    }
  },
  { immediate: true }
)

watch(
  searchKeyRef,
  () => {
    fetchSearchResults()
  },
  { immediate: false }
)

watch(searchResults, async () => {
  await nextTick()
})

onMounted(() => {
  if (props.modelValue !== undefined) {
    setWikidataId(props.modelValue)
  }
  fetchSearchResults()
  if (props.showSaveButton) {
    nextTick(() => {
      const input = document.getElementById(
        "wikidataID-wikidataID"
      ) as HTMLInputElement
      if (input) {
        input.focus()
      }
    })
  }
})
</script>
