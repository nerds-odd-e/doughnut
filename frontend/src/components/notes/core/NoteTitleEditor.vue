<template>
  <div ref="root" class="daisy-form-control note-title-editor">
    <span
      v-if="!hideLabel"
      :id="titleLabelId"
      class="daisy-label"
    >{{ labelText }}</span>
    <h2 class="note-title">
      <SeamlessTextEditor
        :model-value="modelValue"
        :readonly="readonly"
        :aria-labelledby="hideLabel ? undefined : titleLabelId"
        role="title"
        data-test="note-title"
        @update:model-value="emit('update:modelValue', $event)"
        @blur="emit('blur')"
      />
    </h2>
    <div v-if="errorMessage" class="daisy-text-error daisy-text-sm">
      {{ errorMessage }}
    </div>
    <div v-if="$slots.append" class="note-title-editor-append">
      <slot name="append" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from "vue"
import SeamlessTextEditor from "../../form/SeamlessTextEditor.vue"

const titleLabelId = "note-title-field-label"

const props = withDefaults(
  defineProps<{
    modelValue: string
    errorMessage?: string
    readonly?: boolean
    autofocus?: boolean
    /** When false, shows a "Title" label for forms and E2E (findByLabelText). */
    hideLabel?: boolean
    labelText?: string
  }>(),
  {
    readonly: false,
    autofocus: false,
    hideLabel: false,
    labelText: "Title",
  }
)

const emit = defineEmits<{
  "update:modelValue": [value: string]
  blur: []
}>()

const root = ref<HTMLElement | null>(null)

onMounted(() => {
  if (!props.autofocus) return
  nextTick(() => {
    root.value?.querySelector<HTMLElement>(".seamless-editor")?.focus()
  })
})
</script>

<style scoped>
h2.note-title {
  font-size: 1.5rem;
  font-weight: 400;
  margin-bottom: 10px;
}

.note-title-editor-append {
  margin-top: 0.5rem;
}
</style>
