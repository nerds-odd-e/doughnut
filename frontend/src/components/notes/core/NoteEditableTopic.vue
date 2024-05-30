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
        @update:model-value="update(noteTopic.id, $event)"
        @blur="blur"
        :errors="errors.topic"
      >
        <h2><NoteTopicComp v-bind="{ noteTopic }" /></h2>
      </EditableText>
    </template>
  </TextContentWrapper>
</template>

<script setup lang="ts">
import { PropType } from "vue";
import { NoteTopic } from "@/generated/backend";
import EditableText from "../../form/EditableText.vue";
import { type StorageAccessor } from "../../../store/createNoteStorage";
import TextContentWrapper from "./TextContentWrapper.vue";
import NoteTopicComp from "./NoteTopic.vue";

defineProps({
  noteTopic: { type: Object as PropType<NoteTopic>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});
</script>
