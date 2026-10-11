<template>
  <TextContentWrapper
    :value="noteTopology.title"
    field="edit title"
    :title-rename-needs-explicit-reference-choice="hasInboundReferences"
    :note-id="noteId"
    :retain-title-draft="retainTitleDraft"
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
          <h2 class="path-name-heading relative">
            <component :is="editor" v-bind="bindings" />
          </h2>
        </template>
      </PathNameEditor>
    </template>
  </TextContentWrapper>
</template>

<script setup lang="ts">
import { computed, inject, onUnmounted, ref, type PropType } from "vue"
import type { NoteTopology } from "@generated/donut-backend-api"
import { noteTitleDictationKey } from "@/composables/useTitleDictation"
import TextContentWrapper from "./TextContentWrapper.vue"
import PathNameEditor from "./PathNameEditor.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  noteId: { type: Number, required: true },
  readonly: { type: Boolean, default: true },
  hasInboundReferences: { type: Boolean, default: false },
})

const pathNameEditor = ref<InstanceType<typeof PathNameEditor> | null>(null)

const beginDictation = () => pathNameEditor.value!.beginDictation(false)
const noteTitle = inject(noteTitleDictationKey, undefined)
const retainTitleDraft = computed(
  () =>
    noteTitle?.dictation.busy.value === true ||
    noteTitle?.hasTitleIntent.value === true
)

defineExpose({ beginDictation })
onUnmounted(() => noteTitle?.dictation.leave())
</script>

<style scoped>
h2.path-name-heading {
  margin-bottom: 10px;
  font-size: 1.875rem;
  font-weight: 700;
}
</style>
