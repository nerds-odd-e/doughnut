<template>
  <Modal v-if="!noModal" @close_request="handleClose">
    <template #header>
      <h2>Associate Wikidata</h2>
    </template>
    <template #body>
      <WikidataAssociationDialogBody
        ref="bodyRef"
        :search-key="searchKey"
        :model-value="modelValue"
        :error-message="errorMessage"
        :show-save-button="showSaveButton"
        :disabled="disabled"
        :can-save-empty-to-clear="canSaveEmptyToClear"
        :saved-value="savedValue"
        @close="handleClose"
        @save="emit('save', $event)"
        @selected="(e, a) => emit('selected', e, a)"
        @update:model-value="emit('update:modelValue', $event)"
      />
    </template>
  </Modal>
  <template v-else>
    <div class="modal-header">
      <h2>Associate Wikidata</h2>
    </div>
    <div class="modal-body">
      <WikidataAssociationDialogBody
        ref="bodyRef"
        :search-key="searchKey"
        :model-value="modelValue"
        :error-message="errorMessage"
        :show-save-button="showSaveButton"
        :disabled="disabled"
        :can-save-empty-to-clear="canSaveEmptyToClear"
        :saved-value="savedValue"
        @close="handleClose"
        @save="emit('save', $event)"
        @selected="(e, a) => emit('selected', e, a)"
        @update:model-value="emit('update:modelValue', $event)"
      />
    </div>
  </template>
</template>

<script setup lang="ts">
import { ref } from "vue"
import type { WikidataSearchEntity } from "@generated/doughnut-backend-api"
import Modal from "../commons/Modal.vue"
import WikidataAssociationDialogBody from "./WikidataAssociationDialogBody.vue"

defineProps<{
  searchKey: string
  modelValue?: string
  errorMessage?: string
  showSaveButton?: boolean
  disabled?: boolean
  canSaveEmptyToClear?: boolean
  savedValue?: string
  /** When true, render only header + body panels for use inside another Modal (e.g. PopButton). */
  noModal?: boolean
}>()

const emit = defineEmits<{
  close: []
  selected: [entity: WikidataSearchEntity, titleAction?: "replace" | "append"]
  "update:modelValue": [value: string]
  save: [wikidataId: string]
}>()

const bodyRef = ref<InstanceType<typeof WikidataAssociationDialogBody> | null>(
  null
)

const handleClose = () => {
  emit("close")
}

defineExpose({
  showTitleOptionsForEntity: (entity: WikidataSearchEntity) =>
    bodyRef.value?.showTitleOptionsForEntity(entity),
  get showTitleOptions() {
    return bodyRef.value?.showTitleOptions ?? false
  },
})
</script>
