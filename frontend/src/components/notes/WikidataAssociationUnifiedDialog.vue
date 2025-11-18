<template>
  <Modal @close_request="handleClose">
    <template #header>
      <h2>{{ headerTitle }}</h2>
    </template>
    <template #body>
      <form
        v-if="showSaveButton"
        id="wikidata-association-form"
        @submit.prevent="handleSave"
      >
        <div class="daisy-mb-4">
          <TextInput
            scope-name="wikidataID"
            field="wikidataID"
            :model-value="localWikidataId"
            @update:model-value="handleInputChange"
            :error-message="errorMessage"
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
      <div v-else class="daisy-mb-4">
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
        v-else-if="searchResults && searchResults.length === 0 && hasSearched && searchKey"
        class="daisy-text-center daisy-p-4"
      >
        <p>No Wikidata entries found for '{{ searchKey }}'</p>
      </div>
      <div v-else-if="searchResults && searchResults.length > 0 && !showTitleOptions">
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
        <button
          v-if="showSaveButton"
          type="submit"
          form="wikidata-association-form"
          class="daisy-btn daisy-btn-primary"
          :disabled="!localWikidataId || localWikidataId.trim() === ''"
        >
          Save
        </button>
        <button
          v-if="showSaveButton"
          type="button"
          class="daisy-btn daisy-btn-secondary"
          @click="handleClose"
        >
          Close
        </button>
        <button
          v-else
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
import { ref, watch, onMounted, nextTick, computed } from "vue"
import type { WikidataSearchEntity } from "@generated/backend"
import Modal from "../commons/Modal.vue"
import RadioButtons from "../form/RadioButtons.vue"
import TextInput from "../form/TextInput.vue"
import { useWikidataAssociation } from "@/composables/useWikidataAssociation"
import useLoadingApi from "@/managedApi/useLoadingApi"
import nonBlockingPopup from "@/managedApi/window/nonBlockingPopup"
import SvgPopup from "../svgs/SvgPopup.vue"

const props = defineProps<{
  searchKey?: string
  currentTitle: string
  modelValue?: string
  errorMessage?: string
  headerTitle?: string
  showSaveButton?: boolean
}>()

const emit = defineEmits<{
  close: []
  selected: [entity: WikidataSearchEntity, titleAction?: "replace" | "append"]
  "update:modelValue": [value: string]
  save: [wikidataId: string]
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
  showTitleOptionsForEntity,
} = useWikidataAssociation(searchKeyRef, currentTitleRef, props.modelValue)

defineExpose({
  showTitleOptionsForEntity,
})

const select = ref<HTMLSelectElement | null>(null)
const { managedApi } = useLoadingApi()
const isLoadingUrl = ref(false)

const hasValidWikidataId = computed(() => {
  return localWikidataId.value && localWikidataId.value.trim() !== ""
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
  // When showSaveButton is true, don't emit selected immediately
  // Wait for user to click Save button
  if (!props.showSaveButton) {
    emit("selected", selectedItem.value, action)
  }
}

const handleClose = () => {
  emit("close")
}

const handleSave = () => {
  if (localWikidataId.value && localWikidataId.value.trim() !== "") {
    // If title options are shown and user has selected an action,
    // emit selected event instead of save
    if (showTitleOptions.value && selectedItem.value && titleAction.value) {
      const action = getTitleAction()
      emit("selected", selectedItem.value, action)
    } else {
      emit("save", localWikidataId.value)
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

watch(searchResults, async () => {
  await nextTick()
  // Only auto-focus dropdown when NOT in edit mode (showSaveButton is false)
  // In edit mode, keep focus on input field for direct typing
  if (!props.showSaveButton && select.value && searchResults.value.length > 0) {
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
  // In edit mode, focus the input field after mount
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

