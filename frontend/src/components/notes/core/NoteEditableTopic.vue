<template>
  <TextContentWrapper
    :value="noteTopology.titleOrPredicate"
    :storage-accessor="storageAccessor"
    field="edit topic"
  >
    <template #default="{ value, update, blur, errors }">
      <EditableText
        role="topic"
        class="note-title"
        scope-name="note"
        :model-value="value"
        :readonly="readonly"
        @update:model-value="update(noteTopology.id, $event)"
        @blur="blur"
        :error-message="errors.topic"
      >
        <h2><NoteTopicComponent v-bind="{ noteTopology }" /></h2>
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
import NoteTopicComponent from "./NoteTitleComponent.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  readonly: { type: Boolean, default: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
</script>
