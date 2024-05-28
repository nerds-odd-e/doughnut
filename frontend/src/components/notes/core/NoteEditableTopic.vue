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
        @update:model-value="update(note.id, $event)"
        @blur="blur"
        :errors="errors.topic"
      >
        <h2><NoteTopic v-bind="{ note }" /></h2>
      </EditableText>
    </template>
  </TextContentWrapper>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";
import EditableText from "../../form/EditableText.vue";
import { type StorageAccessor } from "../../../store/createNoteStorage";
import TextContentWrapper from "./TextContentWrapper.vue";
import NoteTopic from "./NoteTopic.vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Note>, required: true },
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
