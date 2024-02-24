<template>
  <TextContentWrapper :storage-accessor="storageAccessor" field="edit details">
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
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import RichMarkdownEditor from "../form/RichMarkdownEditor.vue";
import { type StorageAccessor } from "../../store/createNoteStorage";
import TextContentWrapper from "./TextContentWrapper.vue";

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    noteDetails: { type: String, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    RichMarkdownEditor,
    TextContentWrapper,
  },
  data() {
    return {
      localDetails: this.noteDetails,
    };
  },
  watch: {
    noteDetails() {
      this.localDetails = this.noteDetails;
    },
  },
});
</script>
