<template>
  <Modal @close_request="handleClose">
    <template #header>
      <h2>Associate Wikidata</h2>
    </template>
    <template #body>
      <form
        id="wikidata-association-form"
        @submit.prevent="handleSave"
      >
        <div class="daisy-mb-4">
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
                      class="daisy-btn daisy-rounded-l-none"
                      :class="[
                        isLoadingUrl ? 'daisy-btn-disabled' : 'daisy-btn-primary',
                      ]"
                      title="open link"
                      @click="handleOpenLink"
                      :disabled="isLoadingUrl || !hasValidWikidataId"
                      v-show="hasValidWikidataId"
                    >
                      <SvgPopup />
                    </button>
            </template>
          </TextInput>
        </div>
      </form>
      <div v-if="loading" class="daisy-text-center daisy-p-4">
        Searching...
      </div>
      <div
        v-else-if="searchResults && searchResults.length === 0 && hasSearched"
        class="daisy-text-center daisy-p-4"
      >
        <p>No Wikidata entries found for '{{ searchKeyRef }}'</p>
      </div>
      <div v-else-if="searchResults && searchResults.length > 0 && !showTitleOptions">
        <select
          ref="select"
          size="10"
          name="wikidataSearchResult"
          @change="onSelectSearchResult"
          v-model="selectedOption"
          class="daisy-select daisy-select-bordered daisy-w-full"
          :disabled="props.disabled"
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
        <fieldset :disabled="props.disabled">
          <RadioButtons
            v-model="titleAction"
            scope-name="wikidataTitleAction"
            :options="[
              { value: 'Replace', label: 'Replace title' },
              { value: 'Append', label: 'Append title' },
            ]"
            @update:model-value="handleTitleAction"
          />
        </fieldset>
      </div>
      <div class="daisy-mt-4 daisy-flex daisy-gap-2">
        <button
          v-if="hasSaveButton"
          type="submit"
          form="wikidata-association-form"
          class="daisy-btn daisy-btn-primary"
          :disabled="!hasValidWikidataId || props.disabled"
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
  </Modal>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick, computed } from "vue"
import type { WikidataSearchEntity } from "@generated/backend"
import Modal from "../commons/Modal.vue"
import RadioButtons from "../form/RadioButtons.vue"
import TextInput from "../form/TextInput.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import nonBlockingPopup from "@/managedApi/window/nonBlockingPopup"
import SvgPopup from "../svgs/SvgPopup.vue"

const props = defineProps<{
  searchKey: string
  modelValue?: string
  errorMessage?: string
  showSaveButton?: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  close: []
  selected: [entity: WikidataSearchEntity, titleAction?: "replace" | "append"]
  "update:modelValue": [value: string]
  save: [wikidataId: string]
}>()

const { managedApi } = useLoadingApi()

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
const select = ref<HTMLSelectElement | null>(null)
const isLoadingUrl = ref(false)

const hasValidWikidataId = computed(() => {
  return localWikidataId.value && localWikidataId.value.trim() !== ""
})

const fetchSearchResults = async () => {
  loading.value = true
  hasSearched.value = true
  try {
    searchResults.value = await managedApi.services.searchWikidata({
      search: searchKeyRef.value,
    })
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

const getWikidataItem = async (wikidataId: string) => {
  return (
    await managedApi.services.fetchWikidataEntityDataById({
      wikidataId,
    })
  ).WikipediaEnglishUrl
}

const wikiUrl = async (wikidataId: string) => {
  const wikipediaEnglishUrl = await getWikidataItem(wikidataId)
  if (wikipediaEnglishUrl !== "") {
    return wikipediaEnglishUrl
  }
  return `https://www.wikidata.org/wiki/${wikidataId}`
}

const handleOpenLink = () => {
  if (!hasValidWikidataId.value) return
  isLoadingUrl.value = true
  const urlPromise = wikiUrl(localWikidataId.value)
  nonBlockingPopup(urlPromise)
  urlPromise
    .then(() => {
      isLoadingUrl.value = false
    })
    .catch(() => {
      isLoadingUrl.value = false
    })
}

const handleInputChange = (value: string) => {
  setWikidataId(value)
  emit("update:modelValue", value)
}

const onSelectSearchResult = async () => {
  const result = selectSearchResult(selectedOption.value)
  if (!result || !result.entity.id) return

  emit("update:modelValue", result.entity.id)
  // Only emit selected immediately if showSaveButton is false
  // When showSaveButton is true, wait for user to click Save button
  if (!result.needsTitleAction && !props.showSaveButton) {
    emit("selected", result.entity)
  }
}

const handleTitleAction = async () => {
  if (!selectedItem.value) return
  const action = getTitleAction()
  if (!action) return

  // When title action is selected, emit immediately to save and close
  // This applies to both showSaveButton true and false cases
  emit("selected", selectedItem.value, action)
}

const handleClose = () => {
  emit("close")
}

const handleSave = async () => {
  if (!hasValidWikidataId.value) return

  if (hasSaveButton.value) {
    // If title options are shown and action is selected, emit selected
    if (showTitleOptions.value && selectedItem.value && titleAction.value) {
      const action = getTitleAction()
      emit("selected", selectedItem.value, action)
    } else {
      // Otherwise, just save the wikidata ID (no title update)
      emit("save", localWikidataId.value)
    }
  }
  // If hasSaveButton is false, form submission does nothing (just prevents default)
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
  if (!props.showSaveButton && select.value && searchResults.value.length > 0) {
    select.value.focus()
  }
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

