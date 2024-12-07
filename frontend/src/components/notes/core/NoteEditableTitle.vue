<template>
  <TextContentWrapper
    :value="noteTopology.titleOrPredicate"
    :storage-accessor="storageAccessor"
    field="edit title"
  >
    <template #default="{ value, update, blur, errors }">
      <EditableText
        role="title"
        class="note-title"
        scope-name="note"
        :model-value="value"
        :readonly="readonly"
        @update:model-value="update(noteTopology.id, $event)"
        @blur="blur"
        :error-message="errors.title"
      >
        <h2><NoteTitleComponent v-bind="{ noteTopology }" /></h2>
      </EditableText>
    </template>
  </TextContentWrapper>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { NoteTopology } from "@/generated/backend"
import { type StorageAccessor } from "../../../store/createNoteStorage"
import EditableText from "../../form/EditableText.vue"
import TextContentWrapper from "./TextContentWrapper.vue"
import NoteTitleComponent from "./NoteTitleComponent.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  readonly: { type: Boolean, default: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
</script>

<style scoped>
h2 {
  font-size: 1.5rem;
  font-weight: 400;
  margin-bottom: 10px;
}
</style>
