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
          <div class="path-name-heading-line flex items-center gap-2">
            <h2 class="path-name-heading flex-1 min-w-0">
              <component :is="editor" v-bind="bindings" />
            </h2>
            <SpeakTitleControl
              v-if="!readonly"
              @heard-segments="onHeardTitleSegments"
            />
          </div>
        </template>
      </PathNameEditor>
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
.path-name-heading-line {
  margin-bottom: 10px;
}

h2.path-name-heading {
  font-size: 1.875rem;
  font-weight: 700;
}
</style>
