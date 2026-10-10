<template>
  <TextContentWrapper
    :value="noteTopology.title"
    field="edit title"
    :title-rename-needs-explicit-reference-choice="hasInboundReferences"
    :note-id="noteId"
  >
    <template #default="{ value, update, blur, errors }">
      <PathNameEditor
        ref="pathNameEditor"
        :model-value="value || ''"
        :error-message="errors.title"
        :readonly="readonly"
        hide-label
        warn-on-note-title-compatibility
        @update:model-value="update"
        @blur="blur"
      >
        <template #title="{ bindings, editor }">
          <h2 class="path-name-heading">
            <component :is="editor" v-bind="bindings" />
          </h2>
        </template>
      </PathNameEditor>
      <SpeakTitleControl
        v-if="!readonly"
        class="mt-2"
        @heard-segments="onHeardTitleSegments"
      />
    </template>
  </TextContentWrapper>
</template>

<script setup lang="ts">
import { ref, type PropType } from "vue"
import type { NoteTopology } from "@generated/donut-backend-api"
import { dictatedInsertion } from "@/models/audio/joinDictatedSegments"
import SpeakTitleControl from "../SpeakTitleControl.vue"
import TextContentWrapper from "./TextContentWrapper.vue"
import PathNameEditor from "./PathNameEditor.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  noteId: { type: Number, required: true },
  readonly: { type: Boolean, default: true },
  hasInboundReferences: { type: Boolean, default: false },
})

const pathNameEditor = ref<InstanceType<typeof PathNameEditor> | null>(null)

function onHeardTitleSegments(segments: string[]) {
  pathNameEditor.value?.insertAtSelection((before, after) =>
    dictatedInsertion(before, segments, after)
  )
}
</script>

<style scoped>
h2.path-name-heading {
  font-size: 1.875rem;
  font-weight: 700;
  margin-bottom: 10px;
  width: 100%;
}
</style>
