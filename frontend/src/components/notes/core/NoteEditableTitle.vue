<template>
  <TextContentWrapper
    :value="noteTopology.title ?? ''"
    field="edit title"
  >
    <template #default="{ value, update, blur, errors }">
      <NoteTitleEditor
        :model-value="value || ''"
        :error-message="errors.title"
        :readonly="readonly"
        hide-label
        @update:model-value="update(noteTopology.id, $event)"
        @blur="blur"
      />
    </template>
  </TextContentWrapper>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/doughnut-backend-api"
import TextContentWrapper from "./TextContentWrapper.vue"
import NoteTitleEditor from "./NoteTitleEditor.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  readonly: { type: Boolean, default: true },
})
</script>
