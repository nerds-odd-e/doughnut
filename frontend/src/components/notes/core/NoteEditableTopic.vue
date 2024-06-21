<template>
  <TextContentWrapper
    :value="noteTopic.topicConstructor"
    :storage-accessor="storageAccessor"
    field="edit topic"
  >
    <template #default="{ value, update, blur, errors }">
      <EditableText
        role="topic"
        class="note-topic"
        scope-name="note"
        :model-value="value"
        :readonly="readonly"
        @update:model-value="update(noteTopic.id, $event)"
        @blur="blur"
        :errors="errors.topic"
      >
        <h2><NoteTopicComponent v-bind="{ noteTopic }" /></h2>
      </EditableText>
    </template>
  </TextContentWrapper>
</template>

<script setup lang="ts">
import { NoteTopic } from "@/generated/backend"
import { PropType } from "vue"
import { type StorageAccessor } from "../../../store/createNoteStorage"

defineProps({
  noteTopic: { type: Object as PropType<NoteTopic>, required: true },
  readonly: { type: Boolean, default: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
</script>
