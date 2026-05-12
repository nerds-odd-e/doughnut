<template>
  <TextContentWrapper
    :value="noteTopology.title"
    field="edit title"
    :title-rename-needs-explicit-reference-choice="hasInboundReferences"
    :title-edit-note-id="noteId"
  >
    <template #default="{ value, update, blur, errors }">
      <PathNameEditor
        :model-value="value || ''"
        :error-message="errors.title"
        :readonly="readonly"
        hide-label
        @update:model-value="update(noteId, $event)"
        @blur="blur"
      >
        <template #title="{ bindings, editor }">
          <h2 class="path-name-heading">
            <component :is="editor" v-bind="bindings" />
          </h2>
        </template>
      </PathNameEditor>
    </template>
  </TextContentWrapper>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/doughnut-backend-api"
import TextContentWrapper from "./TextContentWrapper.vue"
import PathNameEditor from "./PathNameEditor.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  noteId: { type: Number, required: true },
  readonly: { type: Boolean, default: true },
  hasInboundReferences: { type: Boolean, default: false },
})
</script>

<style scoped>
h2.path-name-heading {
  font-size: 1.5rem;
  font-weight: 400;
  margin-bottom: 10px;
  width: 100%;
}
</style>
