<template>
  <TextContentWrapper
    :value="noteDetails"
    :storage-accessor="storageAccessor"
    field="edit details"
  >
    <template #default="{ value, update, blur }">
      <RichMarkdownEditor
        :multiple-line="true"
        scope-name="note"
        :model-value="value"
        :readonly="readonly"
        @update:model-value="update(noteId, $event)"
        @blur="blur"
      />
    </template>
  </TextContentWrapper>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue"
import { type StorageAccessor } from "../../../store/createNoteStorage"
import RichMarkdownEditor from "../../form/RichMarkdownEditor.vue"
import TextContentWrapper from "./TextContentWrapper.vue"

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    noteDetails: { type: String, required: false },
    readonly: { type: Boolean, default: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    RichMarkdownEditor,
    TextContentWrapper,
  },
})
</script>
