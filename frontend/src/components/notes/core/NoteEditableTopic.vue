<template>
  <TextContentWrapper
    :value="noteTopicConstructor"
    :storage-accessor="storageAccessor"
    field="edit topic"
  >
    <template #default="{ value, update, blur, errors }">
      <EditableText
        role="topic"
        class="note-topic"
        scope-name="note"
        :model-value="value"
        @update:model-value="update(noteId, $event)"
        @blur="blur"
        :errors="errors.topic"
      >
        <h2><NoteTopic v-bind="{ topic: noteTopic }" /></h2>
      </EditableText>
    </template>
  </TextContentWrapper>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import EditableText from "../../form/EditableText.vue";
import { type StorageAccessor } from "../../../store/createNoteStorage";
import TextContentWrapper from "./TextContentWrapper.vue";
import NoteTopic from "./NoteTopic.vue";

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    noteTopicConstructor: { type: String, required: true },
    noteTopic: { type: String, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    EditableText,
    TextContentWrapper,
    NoteTopic,
  },
});
</script>
