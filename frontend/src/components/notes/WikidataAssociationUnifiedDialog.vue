<template>
  <Modal @close_request="handleClose">
    <template #header>
      <h2>{{ headerTitle }}</h2>
    </template>
    <template #body>
      <div class="daisy-mb-4">
        <TextInput
          scope-name="wikidataID"
          field="wikidataID"
          :model-value="localWikidataId"
          @update:model-value="handleInputChange"
          :error-message="errorMessage"
          placeholder="example: `Q1234`"
        />
      </div>
      <div v-if="loading" class="daisy-text-center daisy-p-4">
        Searching...
      </div>
      <div
        v-else-if="searchResults.length === 0 && hasSearched && searchKey"
        class="daisy-text-center daisy-p-4"
      >
        <p>No Wikidata entries found for '{{ searchKey }}'</p>
      </div>
      <div v-else-if="searchResults.length > 0 && !showTitleOptions">
        <select
          ref="select"
          size="10"
          name="wikidataSearchResult"
          @change="onSelectSearchResult"
          v-model="selectedOption"
          class="daisy-select daisy-select-bordered daisy-w-full"
        >
          <option disabled value="">- Choose Wikidata Search Result -</option>
          <option
            v-for="suggestion in searchResults"
            :key="suggestion.id"
            :value="suggestion.id"
          >
            {{ suggestion.label }} - {{ suggestion.description }}
          </option>
        </select>
      </div>
      <div v-else-if="showTitleOptions" class="daisy-p-4">
        <label class="daisy-label">
          <span class="daisy-label-text"
            >Suggested Title: {{ selectedItem?.label }}</span
          >
        </label>
        <RadioButtons
          v-model="titleAction"
          scope-name="wikidataTitleAction"
          :options="[
            { value: 'Replace', label: 'Replace title' },
            { value: 'Append', label: 'Append title' },
          ]"
          @update:model-value="handleTitleAction"
        />
      </div>
      <div class="daisy-mt-4 daisy-flex daisy-gap-2">
        <button class="daisy-btn daisy-btn-secondary" @click="handleClose">
          Close
        </button>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick, computed } from "vue"
import type { WikidataSearchEntity } from "@generated/backend"
import Modal from "../commons/Modal.vue"
import RadioButtons from "../form/RadioButtons.vue"
import TextInput from "../form/TextInput.vue"
import { useWikidataAssociation } from "@/composables/useWikidataAssociation"

const props = defineProps<{
  searchKey?: string
  currentTitle: string
  modelValue?: string
  errorMessage?: string
  headerTitle?: string
}>()

const emit = defineEmits<{
  close: []
  selected: [entity: WikidataSearchEntity, titleAction?: "replace" | "append"]
  "update:modelValue": [value: string]
}>()

const searchKeyRef = computed(() => props.searchKey || "")
const currentTitleRef = computed(() => props.currentTitle)
const headerTitle = computed(() => props.headerTitle || "Search Wikidata")

const {
  localWikidataId,
  loading,
  searchResults,
  selectedOption,
  selectedItem,
  showTitleOptions,
  titleAction,
  hasSearched,
  fetchSearchResults,
  selectSearchResult,
  getTitleAction,
  setWikidataId,
} = useWikidataAssociation(searchKeyRef, currentTitleRef, props.modelValue)

const select = ref<HTMLSelectElement | null>(null)

const handleInputChange = (value: string) => {
  setWikidataId(value)
  emit("update:modelValue", value)
}

const onSelectSearchResult = () => {
  const result = selectSearchResult(selectedOption.value)
  if (!result || !result.entity.id) return

  emit("update:modelValue", result.entity.id)

  if (!result.needsTitleAction) {
    emit("selected", result.entity)
  }
}

const handleTitleAction = () => {
  if (!selectedItem.value) return
  const action = getTitleAction()
  emit("selected", selectedItem.value, action)
}

const handleClose = () => {
  emit("close")
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

watch(searchResults, async () => {
  await nextTick()
  if (select.value && searchResults.value.length > 0) {
    select.value.focus()
  }
})

onMounted(() => {
  if (props.modelValue !== undefined) {
    setWikidataId(props.modelValue)
  }
  if (props.searchKey) {
    fetchSearchResults()
  }
})
</script>

