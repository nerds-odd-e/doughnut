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
            <h2 class="path-name-heading relative flex-1 min-w-0">
              <component :is="editor" v-bind="bindings" />
            </h2>
            <SpeakTitleControl
              v-if="!readonly"
              :key="noteId"
              v-model:busy="titleSpeechBusy"
              @heard-segments="insertHeardSegments"
            />
          </div>
        </template>
      </PathNameEditor>
    </template>
  </TextContentWrapper>
</template>

<script setup lang="ts">
import { ref, watch, type PropType } from "vue"
import type { NoteTopology } from "@generated/donut-backend-api"
import { useTitleDictation } from "@/composables/useTitleDictation"
import SpeakTitleControl from "../SpeakTitleControl.vue"
import TextContentWrapper from "./TextContentWrapper.vue"
import PathNameEditor from "./PathNameEditor.vue"

const props = defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  noteId: { type: Number, required: true },
  readonly: { type: Boolean, default: true },
  hasInboundReferences: { type: Boolean, default: false },
})

const pathNameEditor = ref<InstanceType<typeof PathNameEditor> | null>(null)

const {
  busy: titleSpeechBusy,
  insertHeardSegments,
  leave,
} = useTitleDictation(() => pathNameEditor.value!.beginDictation(false))

// Moving to another note stops the listening and ends its session.
watch(() => props.noteId, leave)
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
