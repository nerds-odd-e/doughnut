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
        <span
          class="daisy-tab daisy-tab-active"
          role="tab"
          aria-selected="true"
          data-testid="rich-note-property-value-popup-mode-text"
        >
          Text
        </span>
      </div>
      <textarea
        v-model="draft"
        class="daisy-textarea daisy-textarea-bordered w-full font-mono text-sm"
        rows="6"
        :aria-label="`Property value for ${propertyKey}`"
        data-testid="rich-note-property-value-popup-textarea"
      />
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
import { ref, watch } from "vue"
import Modal from "@/components/commons/Modal.vue"

const props = defineProps<{
  propertyKey: string
  modelValue: string
}>()

const emit = defineEmits<{
  save: [value: string]
  cancel: []
}>()

const draft = ref(props.modelValue)

watch(
  () => props.modelValue,
  (value) => {
    draft.value = value
  }
)

function onSave() {
  emit("save", draft.value)
}

function onCancel() {
  emit("cancel")
}
</script>
