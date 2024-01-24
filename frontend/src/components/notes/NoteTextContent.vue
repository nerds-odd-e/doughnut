<template>
  <div style="display: flex">
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
        />
      </template>
    </TextContentWrapper>
    <slot name="topic-additional" />
  </div>
  <div role="details" class="note-content">
    <TextContentWrapper
      :storage-accessor="storageAccessor"
      field="edit details"
    >
      <template #default="{ update, blur }">
        <RichMarkdownEditor
          :multiple-line="true"
          scope-name="note"
          :model-value="localDetails"
          @update:model-value="
            update(noteId, $event);
            localDetails = $event;
          "
          @blur="blur"
        />
      </template>
    </TextContentWrapper>
    <slot name="note-content-other" />
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import EditableText from "../form/EditableText.vue";
import RichMarkdownEditor from "../form/RichMarkdownEditor.vue";
import { type StorageAccessor } from "../../store/createNoteStorage";
import TextContentWrapper from "./TextContentWrapper.vue";

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    topicConstructor: { type: String, required: true },
    details: { type: String, required: false },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    EditableText,
    RichMarkdownEditor,
    TextContentWrapper,
  },
  data() {
    return {
      localTopicConstructor: this.topicConstructor,
      localDetails: this.details,
    };
  },
  watch: {
    topicConstructor() {
      this.localTopicConstructor = this.topicConstructor;
    },
    details() {
      this.localDetails = this.details;
    },
  },
});
</script>
