<template>
  <TextContentWrapper
    :value="noteTopology.title ?? ''"
    field="edit title"
  >
    <template #default="{ value, update, blur, errors }">
      <h2 class="note-title">
        <SeamlessTextEditor
          :model-value="value || ''"
          :readonly="readonly"
          role="title"
          @update:model-value="update(noteTopology.id, $event)"
          @blur="blur"
        />
      </h2>
      <span v-if="errors.title" class="error-message">{{ errors.title }}</span>
    </template>
  </TextContentWrapper>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/backend"
import TextContentWrapper from "./TextContentWrapper.vue"
import SeamlessTextEditor from "../../form/SeamlessTextEditor.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  readonly: { type: Boolean, default: true },
})
</script>

<style scoped>
h2 {
  font-size: 1.5rem;
  font-weight: 400;
  margin-bottom: 10px;
}

.error-message {
  color: red;
  font-size: 0.875rem;
}
</style>
