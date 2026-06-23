<template>
  <Modal @close_request="onCancel">
    <template #header>
      <h2 class="text-lg font-semibold">{{ propertyKey }}</h2>
    </template>
    <template #body>
      <div
        class="daisy-tabs daisy-tabs-box mb-3"
        role="tablist"
        aria-label="Property value mode"
      >
        <button
          type="button"
          class="daisy-tab"
          :class="{ 'daisy-tab-active': mode === 'text' }"
          role="tab"
          :aria-selected="mode === 'text'"
          data-testid="rich-note-property-value-popup-mode-text"
          @click="mode = 'text'"
        >
          Text
        </button>
        <button
          v-if="listModeAllowed"
          type="button"
          class="daisy-tab"
          :class="{ 'daisy-tab-active': mode === 'list' }"
          role="tab"
          :aria-selected="mode === 'list'"
          data-testid="rich-note-property-value-popup-mode-list"
          @click="mode = 'list'"
        >
          List
        </button>
      </div>
      <textarea
        v-if="mode === 'text'"
        v-model="draftText"
        class="daisy-textarea daisy-textarea-bordered w-full font-mono text-sm"
        rows="6"
        :aria-label="`Property value for ${propertyKey}`"
        data-testid="rich-note-property-value-popup-textarea"
      />
      <div v-else class="flex flex-col gap-2">
        <div
          v-for="(_, index) in draftListItems"
          :key="index"
          class="flex items-center gap-2"
        >
          <input
            v-model="draftListItems[index]"
            type="text"
            class="daisy-input daisy-input-bordered daisy-input-sm min-w-0 flex-1 font-mono text-sm"
            :aria-label="`List item ${index + 1} for ${propertyKey}`"
            :data-testid="`rich-note-property-value-popup-list-item-${index}`"
          />
          <button
            type="button"
            class="daisy-btn daisy-btn-ghost daisy-btn-sm square shrink-0"
            :aria-label="`Remove list item ${index + 1}`"
            :data-testid="`rich-note-property-value-popup-list-remove-${index}`"
            @click="removeListItem(index)"
          >
            <Minus class="h-4 w-4" aria-hidden="true" />
          </button>
        </div>
        <button
          type="button"
          class="daisy-btn daisy-btn-ghost daisy-btn-sm self-start"
          data-testid="rich-note-property-value-popup-list-add"
          @click="addListItem"
        >
          Add item
        </button>
      </div>
      <p
        v-if="validationMessage"
        role="alert"
        class="text-error text-xs mt-2"
        data-testid="rich-note-property-value-popup-validation"
      >
        {{ validationMessage }}
      </p>
      <div class="mt-4 flex justify-end gap-2">
        <button
          type="button"
          class="daisy-btn daisy-btn-ghost"
          data-testid="rich-note-property-value-popup-cancel"
          @click="onCancel"
        >
          Cancel
        </button>
        <button
          type="button"
          class="daisy-btn daisy-btn-primary"
          data-testid="rich-note-property-value-popup-save"
          @click="onSave"
        >
          Save
        </button>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { Minus } from "@lucide/vue"
import { ref, watch } from "vue"
import Modal from "@/components/commons/Modal.vue"
import {
  listPropertyValue,
  scalarPropertyValue,
  scalarStringFromPropertyValue,
  type PropertyValue,
} from "@/utils/noteProperties"

const props = defineProps<{
  propertyKey: string
  propertyValue: PropertyValue
  listModeAllowed: boolean
}>()

const emit = defineEmits<{
  save: [value: PropertyValue]
  cancel: []
}>()

type Mode = "text" | "list"

function draftStateFromPropertyValue(value: PropertyValue) {
  const scalar = scalarStringFromPropertyValue(value) ?? ""
  if (value.kind === "list") {
    return {
      mode: "list" as const,
      draftText: scalar,
      draftListItems: [...value.items],
    }
  }
  return { mode: "text" as const, draftText: scalar, draftListItems: [scalar] }
}

const initialDraft = draftStateFromPropertyValue(props.propertyValue)
const mode = ref<Mode>(initialDraft.mode)
const draftText = ref(initialDraft.draftText)
const draftListItems = ref<string[]>(initialDraft.draftListItems)
const validationMessage = ref("")

watch(
  () => props.propertyValue,
  (value) => {
    const draft = draftStateFromPropertyValue(value)
    mode.value = draft.mode
    draftText.value = draft.draftText
    draftListItems.value = draft.draftListItems
    validationMessage.value = ""
  }
)

function addListItem() {
  draftListItems.value = [...draftListItems.value, ""]
}

function removeListItem(index: number) {
  draftListItems.value = draftListItems.value.filter((_, i) => i !== index)
}

function onSave() {
  validationMessage.value = ""
  if (mode.value === "text") {
    emit("save", scalarPropertyValue(draftText.value))
    return
  }

  if (draftListItems.value.some((item) => item.trim() === "")) {
    validationMessage.value = "List items cannot be empty."
    return
  }

  emit(
    "save",
    listPropertyValue(draftListItems.value.map((item) => item.trim()))
  )
}

function onCancel() {
  emit("cancel")
}
</script>
