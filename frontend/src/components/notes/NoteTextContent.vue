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
            update(note.id, $event);
            localTopicConstructor = $event;
          "
          @blur="blur"
          :errors="errors.topic"
        >
          <h2><NoteTopic v-bind="{ note, parentNote }" /></h2>
        </EditableText>
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
            update(note.id, $event);
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
import NoteTopic from "./NoteTopic.vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    parentNote: { type: Object as PropType<Generated.Note> },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    EditableText,
    RichMarkdownEditor,
    TextContentWrapper,
    NoteTopic,
  },
  data() {
    return {
      localTopicConstructor: this.note.topicConstructor,
      localDetails: this.note.details,
    };
  },
  watch: {
    topicConstructor() {
      this.localTopicConstructor = this.note.topicConstructor;
    },
    details() {
      this.localDetails = this.note.details;
    },
  },
});
</script>
