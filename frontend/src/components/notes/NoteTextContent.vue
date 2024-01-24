<template>
  <div style="display: flex">
    <TextContentWrapper :storage-accessor="storageAccessor" field="edit topic">
      <template #default="{ update, blur, errors }">
        <EditableText
          role="topic"
          class="note-topic"
          scope-name="note"
          :model-value="localTextContent.topic"
          @update:model-value="update(noteId, $event)"
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
          :model-value="localTextContent.details"
          @update:model-value="update(noteId, $event)"
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
    textContent: {
      type: Object as PropType<Generated.TextContent>,
      required: true,
    },
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
      localTextContent: { ...this.textContent } as Generated.TextContent,
    };
  },
  watch: {
    textContent: {
      handler(newValue) {
        this.localTextContent = { ...newValue };
      },
      deep: true,
    },
  },
});
</script>
