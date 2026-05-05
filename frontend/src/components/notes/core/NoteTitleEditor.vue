<template>
  <div ref="root" class="daisy-form-control note-title-editor">
    <div v-if="$slots.append" class="daisy-join daisy-w-full note-title-editor-join">
      <div
        class="note-title-editor-join-editor daisy-join-item daisy-flex daisy-flex-1 daisy-min-w-0 daisy-items-center daisy-border daisy-border-base-content/20 daisy-bg-base-100 daisy-px-3 daisy-py-2 daisy-rounded-l-lg"
      >
        <h2 class="note-title note-title--inline">
          <SeamlessTextEditor
            :model-value="modelValue"
            :readonly="readonly"
            :placeholder="placeholder"
            :aria-label="hideLabel ? undefined : labelText"
            role="title"
            data-test="note-title"
            @update:model-value="emit('update:modelValue', $event)"
            @blur="emit('blur')"
          />
        </h2>
      </div>
      <div
        class="note-title-editor-join-append daisy-join-item daisy-flex daisy-shrink-0 daisy-self-stretch daisy-items-stretch"
      >
        <slot name="append" />
      </div>
    </div>

    <h2 v-else class="note-title">
      <SeamlessTextEditor
        :model-value="modelValue"
        :readonly="readonly"
        :placeholder="placeholder"
        :aria-label="hideLabel ? undefined : labelText"
        role="title"
        data-test="note-title"
        @update:model-value="emit('update:modelValue', $event)"
        @blur="emit('blur')"
      />
    </h2>

    <div v-if="errorMessage" class="daisy-text-error daisy-text-sm">
      {{ errorMessage }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from "vue"
import SeamlessTextEditor from "../../form/SeamlessTextEditor.vue"

const props = withDefaults(
  defineProps<{
    modelValue: string
    errorMessage?: string
    readonly?: boolean
    autofocus?: boolean
    /** When true (e.g. on the note page), no form field name is exposed on the editor. */
    hideLabel?: boolean
    /** Accessible name for the editor when hideLabel is false (forms / E2E). */
    labelText?: string
    placeholder?: string
    /** Select all text once after mount (e.g. default "Untitled" in new-note dialog). */
    initialSelectAll?: boolean
  }>(),
  {
    readonly: false,
    autofocus: false,
    hideLabel: false,
    labelText: "Title",
    initialSelectAll: false,
  }
)

const emit = defineEmits<{
  "update:modelValue": [value: string]
  blur: []
}>()

const root = ref<HTMLElement | null>(null)

function focusEditorAndMaybeSelectAll() {
  const el = root.value?.querySelector<HTMLElement>(".seamless-editor")
  el?.focus()
  if (!props.initialSelectAll || !el) return
  const range = document.createRange()
  range.selectNodeContents(el)
  const sel = window.getSelection()
  sel?.removeAllRanges()
  sel?.addRange(range)
}

onMounted(() => {
  if (!props.autofocus) return
  nextTick(() => {
    focusEditorAndMaybeSelectAll()
  })
})
</script>

<style scoped>
h2.note-title {
  font-size: 1.5rem;
  font-weight: 400;
  margin-bottom: 10px;
}

h2.note-title--inline {
  margin-bottom: 0;
  width: 100%;
}

.note-title-editor-join-append :deep(button) {
  height: 100%;
  min-height: 2.75rem;
}
</style>
