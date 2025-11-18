<template>
  <Modal @close_request="handleClose">
    <template #header>
      <h2>{{ headerTitle }}</h2>
    </template>
    <template #body>
      <form
        v-if="showSaveButton || isAutoSaveMode"
        id="wikidata-association-form"
        @submit.prevent="handleSave"
      >
        <div class="daisy-mb-4">
          <TextInput
            scope-name="wikidataID"
            field="wikidataID"
            :model-value="localWikidataId"
            @update:model-value="handleInputChange"
            :error-message="isAutoSaveMode ? wikidataIdError : errorMessage"
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
            :error-message="isAutoSaveMode ? wikidataIdError : errorMessage"
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
          v-if="showSaveButton || isAutoSaveMode"
          type="submit"
          form="wikidata-association-form"
          class="daisy-btn daisy-btn-primary"
          :disabled="!localWikidataId || localWikidataId.trim() === ''"
        >
          Save
        </button>
        <button
          v-if="showSaveButton || isAutoSaveMode"
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
import type {
  WikidataSearchEntity,
  Note,
  WikidataAssociationCreation,
} from "@generated/backend"
import type { StorageAccessor } from "@/store/createNoteStorage"
import Modal from "../commons/Modal.vue"
import RadioButtons from "../form/RadioButtons.vue"
import TextInput from "../form/TextInput.vue"
import { useWikidataAssociation } from "@/composables/useWikidataAssociation"
import useLoadingApi from "@/managedApi/useLoadingApi"
import nonBlockingPopup from "@/managedApi/window/nonBlockingPopup"
import SvgPopup from "../svgs/SvgPopup.vue"

interface WikidataError {
  body: {
    message: string
  }
}

interface WikidataIdError {
  wikidataId: string
}

const props = defineProps<{
  searchKey?: string
  currentTitle?: string
  modelValue?: string
  errorMessage?: string
  headerTitle?: string
  showSaveButton?: boolean
  note?: Note
  storageAccessor?: StorageAccessor
}>()

const emit = defineEmits<{
  close: []
  closeDialog: []
  selected: [entity: WikidataSearchEntity, titleAction?: "replace" | "append"]
  "update:modelValue": [value: string]
  save: [wikidataId: string]
}>()

const searchKeyRef = computed(() => {
  if (isAutoSaveMode.value) {
    return props.note?.noteTopology.titleOrPredicate || ""
  }
  return props.searchKey || ""
})
const currentTitleRef = computed(() => {
  if (isAutoSaveMode.value) {
    return props.note?.noteTopology.titleOrPredicate || ""
  }
  return props.currentTitle || ""
})
const headerTitle = computed(() => {
  if (props.headerTitle) return props.headerTitle
  if (isAutoSaveMode.value && props.note?.wikidataId) {
    return "Edit Wikidata Association"
  }
  return "Associate Wikidata"
})

const isAutoSaveMode = computed(() => !!props.note && !!props.storageAccessor)
const localWikidataIdForEdit = ref(props.note?.wikidataId || "")
const wikidataIdError = ref<string | undefined>(undefined)

const initialWikidataId = computed(() => {
  if (isAutoSaveMode.value) {
    return localWikidataIdForEdit.value
  }
  return props.modelValue
})

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
} = useWikidataAssociation(
  searchKeyRef,
  currentTitleRef,
  initialWikidataId.value
)

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
  if (isAutoSaveMode.value) {
    localWikidataIdForEdit.value = value
    wikidataIdError.value = undefined
  } else {
    emit("update:modelValue", value)
  }
}

const onSelectSearchResult = async () => {
  const result = selectSearchResult(selectedOption.value)
  if (!result || !result.entity.id) return

  if (isAutoSaveMode.value) {
    localWikidataIdForEdit.value = result.entity.id
    wikidataIdError.value = undefined
    if (!result.needsTitleAction) {
      await handleSelectedForEdit(result.entity)
    }
  } else {
    emit("update:modelValue", result.entity.id)
    if (!result.needsTitleAction) {
      emit("selected", result.entity)
    }
  }
}

const handleTitleAction = async () => {
  if (!selectedItem.value) return
  const action = getTitleAction()
  if (isAutoSaveMode.value && props.showSaveButton) {
    // In auto-save mode with showSaveButton, wait for user to click Save
    // Don't save immediately when title action is selected
  } else if (isAutoSaveMode.value) {
    // In auto-save mode without showSaveButton, save immediately
    await handleSelectedForEdit(selectedItem.value, action)
  } else if (!props.showSaveButton) {
    // When showSaveButton is true, don't emit selected immediately
    // Wait for user to click Save button
    emit("selected", selectedItem.value, action)
  }
}

const handleClose = () => {
  if (isAutoSaveMode.value) {
    emit("closeDialog")
  } else {
    emit("close")
  }
}

const isSaving = ref(false)
const saveWikidataId = async (wikidataId: string) => {
  if (!props.note || !props.storageAccessor || isSaving.value) return
  isSaving.value = true
  try {
    const associationData: WikidataAssociationCreation = {
      wikidataId,
    }
    await props.storageAccessor
      .storedApi()
      .updateWikidataId(props.note.id, associationData)
    emit("closeDialog")
  } catch (e: unknown) {
    isSaving.value = false
    if (typeof e === "object" && e !== null && "wikidataId" in e) {
      wikidataIdError.value = (e as WikidataIdError).wikidataId
    } else {
      wikidataIdError.value = "An unknown error occurred"
    }
    // Keep dialog open to show error
  }
}

const handleSelectedForEdit = async (
  entity: WikidataSearchEntity,
  _titleAction?: "replace" | "append"
) => {
  if (!entity.id || !props.note) return

  // Validate the Wikidata ID
  try {
    const res = await managedApi.services.fetchWikidataEntityDataById({
      wikidataId: entity.id,
    })

    // Check if titles match (case-insensitive)
    const noteTitleUpper =
      props.note.noteTopology.titleOrPredicate.toUpperCase()
    const wikidataTitleUpper = res.WikidataTitleInEnglish.toUpperCase()

    if (
      wikidataTitleUpper === noteTitleUpper ||
      res.WikidataTitleInEnglish === ""
    ) {
      // Titles match or empty - save directly
      await saveWikidataId(entity.id)
    } else {
      // Titles differ - the unified dialog already showed Replace/Append options
      // User has chosen an action, so we save with that understanding
      // (In edit flow, we don't actually change the note title, but the user
      // has acknowledged the difference via Replace/Append choice)
      await saveWikidataId(entity.id)
    }
  } catch (e: unknown) {
    if (
      e instanceof Error &&
      "body" in e &&
      typeof e.body === "object" &&
      e.body &&
      "message" in e.body
    ) {
      wikidataIdError.value = (e as WikidataError).body.message
    } else {
      wikidataIdError.value = "An unknown error occurred"
    }
    // Keep dialog open to show error
  }
}

const handleManualSaveForEdit = async (wikidataId: string) => {
  if (!props.note) return
  // Validate the Wikidata ID
  try {
    const res = await managedApi.services.fetchWikidataEntityDataById({
      wikidataId,
    })

    // Check if titles match (case-insensitive)
    const noteTitleUpper =
      props.note.noteTopology.titleOrPredicate.toUpperCase()
    const wikidataTitleUpper = res.WikidataTitleInEnglish.toUpperCase()

    if (
      wikidataTitleUpper === noteTitleUpper ||
      res.WikidataTitleInEnglish === ""
    ) {
      // Titles match or empty - save directly
      await saveWikidataId(wikidataId)
    } else {
      // Titles differ - show Replace/Append options via unified dialog
      const entity: WikidataSearchEntity = {
        id: wikidataId,
        label: res.WikidataTitleInEnglish,
        description: "",
      }
      showTitleOptionsForEntity(entity)
      await nextTick() // Ensure Vue updates the DOM
      // Don't save yet - wait for user to select Replace/Append option
      // The handleTitleAction will trigger handleSelectedForEdit which will save
    }
  } catch (e: unknown) {
    if (
      e instanceof Error &&
      "body" in e &&
      typeof e.body === "object" &&
      e.body &&
      "message" in e.body
    ) {
      wikidataIdError.value = (e as WikidataError).body.message
    } else {
      wikidataIdError.value = "An unknown error occurred"
    }
    // Keep dialog open to show error
  }
}

const handleSave = async () => {
  if (localWikidataId.value && localWikidataId.value.trim() !== "") {
    if (isAutoSaveMode.value) {
      // If title options are shown and user has selected an action,
      // use selected event handler
      if (showTitleOptions.value && selectedItem.value && titleAction.value) {
        const action = getTitleAction()
        await handleSelectedForEdit(selectedItem.value, action)
      } else {
        await handleManualSaveForEdit(localWikidataId.value)
      }
    } else if (
      showTitleOptions.value &&
      selectedItem.value &&
      titleAction.value
    ) {
      // If title options are shown and user has selected an action,
      // emit selected event instead of save
      const action = getTitleAction()
      emit("selected", selectedItem.value, action)
    } else {
      emit("save", localWikidataId.value)
    }
  }
}

watch(
  () =>
    isAutoSaveMode.value ? localWikidataIdForEdit.value : props.modelValue,
  (newValue) => {
    if (newValue !== undefined) {
      setWikidataId(newValue)
    }
  },
  { immediate: true }
)

watch(
  () => props.note?.wikidataId,
  (newValue) => {
    if (isAutoSaveMode.value && newValue !== undefined) {
      localWikidataIdForEdit.value = newValue
      setWikidataId(newValue)
    }
  }
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
  const initialValue = isAutoSaveMode.value
    ? localWikidataIdForEdit.value
    : props.modelValue
  if (initialValue !== undefined) {
    setWikidataId(initialValue)
  }
  const searchKey = isAutoSaveMode.value
    ? props.note?.noteTopology.titleOrPredicate
    : props.searchKey
  if (searchKey) {
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

