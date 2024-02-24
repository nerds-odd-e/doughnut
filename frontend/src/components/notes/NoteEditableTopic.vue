<template>
  <TextContentWrapper :storage-accessor="storageAccessor" field="edit topic">
    <template #default="{ update, blur, errors }">
      <EditableText
        role="topic"
        class="note-topic"
        scope-name="note"
        :model-value="localTopicConstructor"
        @update:model-value="
          update(noteId, $event);
          localTopicConstructor = $event;
        "
        @blur="blur"
        :errors="errors.topic"
      >
        <h2><NoteTopic v-bind="{ topic: noteTopicConstructor }" /></h2>
      </EditableText>
    </template>
  </TextContentWrapper>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import EditableText from "../form/EditableText.vue";
import { type StorageAccessor } from "../../store/createNoteStorage";
import TextContentWrapper from "./TextContentWrapper.vue";
import NoteTopic from "./NoteTopic.vue";

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    noteTopicConstructor: { type: String, required: true },
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
  data() {
    return {
      localTopicConstructor: this.noteTopicConstructor,
    };
  },
  watch: {
    note() {
      this.localTopicConstructor = this.noteTopicConstructor;
    },
  },
});
</script>
