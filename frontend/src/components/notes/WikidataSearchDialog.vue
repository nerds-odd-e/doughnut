<template>
  <Modal @close_request="handleClose">
    <template #header>
      <h2>Search Wikidata</h2>
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
      <div v-else-if="searchResults.length === 0 && hasSearched" class="daisy-text-center daisy-p-4">
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
          <span class="daisy-label-text">Suggested Title: {{ selectedItem?.label }}</span>
        </label>
        <RadioButtons
          v-model="titleAction"
          scope-name="wikidataTitleAction"
          :options="[
            { value: 'Replace', label: 'Replace title' },
            { value: 'Append', label: 'Append title' },
            { value: 'Neither', label: 'Neither' },
          ]"
          @update:model-value="handleTitleAction"
        />
      </div>
      <div class="daisy-mt-4 daisy-flex daisy-gap-2">
        <button
          class="daisy-btn daisy-btn-secondary"
          @click="handleClose"
        >
          Close
        </button>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick } from "vue"
import type { WikidataSearchEntity } from "@generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import Modal from "../commons/Modal.vue"
import RadioButtons from "../form/RadioButtons.vue"
import TextInput from "../form/TextInput.vue"

const props = defineProps<{
  searchKey: string
  currentTitle: string
  modelValue?: string
  errorMessage?: string
}>()

const emit = defineEmits<{
  close: []
  selected: [
    entity: WikidataSearchEntity,
    titleAction?: "replace" | "append" | "neither",
  ]
  "update:modelValue": [value: string]
}>()

const { managedApi } = useLoadingApi()

const loading = ref(false)
const searchResults = ref<WikidataSearchEntity[]>([])
const selectedOption = ref("")
const selectedItem = ref<WikidataSearchEntity | null>(null)
const showTitleOptions = ref(false)
const titleAction = ref<"Replace" | "Append" | "Neither" | "">("")
const select = ref<HTMLSelectElement | null>(null)
const localWikidataId = ref(props.modelValue || "")
const hasSearched = ref(false)

const handleInputChange = (value: string) => {
  localWikidataId.value = value
  emit("update:modelValue", value)
}

const fetchSearchResults = async () => {
  if (!props.searchKey) return
  loading.value = true
  hasSearched.value = true
  try {
    searchResults.value = await managedApi.services.searchWikidata({
      search: props.searchKey,
    })
    await nextTick()
    if (select.value && searchResults.value.length > 0) {
      select.value.focus()
    }
  } finally {
    loading.value = false
  }
}

const onSelectSearchResult = () => {
  const selected = searchResults.value.find(
    (obj) => obj.id === selectedOption.value
  )
  if (!selected) return
  const selectedId = selected.id
  if (!selectedId) return

  selectedItem.value = selected
  localWikidataId.value = selectedId
  emit("update:modelValue", selectedId)

  const currentLabel = props.currentTitle.toUpperCase()
  const newLabel = selected.label.toUpperCase()

  if (currentLabel === newLabel) {
    emit("selected", selected)
  } else {
    showTitleOptions.value = true
  }
}

const handleTitleAction = () => {
  if (!selectedItem.value) return

  let action: "replace" | "append" | "neither" | undefined
  if (titleAction.value === "Replace") {
    action = "replace"
  } else if (titleAction.value === "Append") {
    action = "append"
  } else if (titleAction.value === "Neither") {
    action = "neither"
  }

  emit("selected", selectedItem.value, action)
}

const handleClose = () => {
  emit("close")
}

watch(
  () => props.modelValue,
  (newValue) => {
    if (newValue !== undefined) {
      localWikidataId.value = newValue
    }
  },
  { immediate: true }
)

watch(
  () => props.searchKey,
  () => {
    if (props.searchKey) {
      fetchSearchResults()
    }
  },
  { immediate: false }
)

onMounted(() => {
  if (props.modelValue !== undefined) {
    localWikidataId.value = props.modelValue
  }
  if (props.searchKey) {
    fetchSearchResults()
  }
})
</script>

