<template>
  <div style="display: flex">
    <NoteEditableTopic
      :note-id="note.id"
      :note-topic-constructor="note.topicConstructor"
      :storage-accessor="storageAccessor"
    />
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
import RichMarkdownEditor from "../form/RichMarkdownEditor.vue";
import { type StorageAccessor } from "../../store/createNoteStorage";
import TextContentWrapper from "./TextContentWrapper.vue";
import NoteEditableTopic from "./NoteEditableTopic.vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    RichMarkdownEditor,
    TextContentWrapper,
    NoteEditableTopic,
  },
  data() {
    return {
      localTopicConstructor: this.note.topicConstructor,
      localDetails: this.note.details,
    };
  },
  watch: {
    note() {
      this.localTopicConstructor = this.note.topicConstructor;
      this.localDetails = this.note.details;
    },
  },
});
</script>
