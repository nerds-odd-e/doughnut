<template>
  <TextContentWrapper
    :value="noteDetails"
    :storage-accessor="storageAccessor"
    field="edit details"
  >
    <template #default="{ value, update, blur }">
      <TextArea
        v-if="asMarkdown"
        :multiple-line="true"
        scope-name="note"
        :model-value="value"
        :readonly="readonly"
        :auto-extend-until="1000"
        @update:model-value="update(noteId, $event)"
        @blur="blur"
      />
      <RichMarkdownEditor
        v-else
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

<script setup lang="ts">
import type { PropType } from "vue"
import type { StorageAccessor } from "../../../store/createNoteStorage"
import RichMarkdownEditor from "../../form/RichMarkdownEditor.vue"
import TextContentWrapper from "./TextContentWrapper.vue"
import TextArea from "@/components/form/TextArea.vue"

defineProps({
  noteId: { type: Number, required: true },
  noteDetails: { type: String, required: false },
  readonly: { type: Boolean, default: true },
  asMarkdown: Boolean,
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
</script>
