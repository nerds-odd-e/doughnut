<template>
  <div
    ref="root"
    class="daisy-form-control path-name-editor"
    :data-autofocus="autofocus ? 'true' : undefined"
    :data-autofocus-select-all="initialSelectAll ? 'true' : undefined"
  >
    <div v-if="$slots.append" class="daisy-join w-full path-name-editor-join">
      <div
        class="path-name-editor-join-editor daisy-join-item doughnut-field-control-surface flex flex-1 min-w-0 items-center px-3 py-2"
      >
        <div class="w-full min-w-0">
          <slot name="title" :bindings="seamlessBindings" :editor="SeamlessTextEditor">
            <SeamlessTextEditor v-bind="seamlessBindings" />
          </slot>
        </div>
      </div>
      <div
        class="path-name-editor-join-append flex shrink-0 self-stretch items-stretch"
      >
        <slot name="append" />
      </div>
    </div>

    <div
      v-else-if="!$slots.title"
      class="path-name-editor-shell doughnut-field-control-surface w-full min-w-0 flex items-center px-3 py-2 rounded-lg"
    >
      <div class="w-full min-w-0">
        <SeamlessTextEditor v-bind="seamlessBindings" />
      </div>
    </div>

    <slot
      v-else
      name="title"
      :bindings="seamlessBindings"
      :editor="SeamlessTextEditor"
    />

    <div v-if="errorMessage" class="text-error text-sm">
      {{ errorMessage }}
    </div>
    <div v-else-if="displayWarning" class="text-warning text-sm">
      {{ displayWarning }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import SeamlessTextEditor from "../../form/SeamlessTextEditor.vue"
import { scheduleFocusTargetWithin } from "@/utils/focusTarget"

const FULLWIDTH_REPLACE: Record<string, string> = {
  "\\": "＼",
  "/": "／",
  ":": "：",
}

const LINK_BREAK_CHARS = "#^[]|"

const LINK_NAME_WARNING =
  "Links will not work with names containing any of `#^[]|`"

function processIllegalPathChars(raw: string): {
  value: string
  replacementNote: string
} {
  let replacementNote = ""
  let value = ""
  for (const c of raw) {
    const full = FULLWIDTH_REPLACE[c]
    if (full !== undefined) {
      value += full
      if (!replacementNote) {
        replacementNote = `'${c}' is not a legal name, and it has been replaced with the fullwidth '${full}'`
      }
    } else {
      value += c
    }
  }
  return { value, replacementNote }
}

function hasLinkBreakChars(s: string): boolean {
  for (const c of s) {
    if (LINK_BREAK_CHARS.includes(c)) return true
  }
  return false
}

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
    /** Passed to the inner editor (e.g. `title` on the note page, `textbox` in dialogs). */
    editorRole?: string
    /** `data-test` on the inner editor (E2E note flows use `note-title`). */
    editorDataTest?: string
  }>(),
  {
    readonly: false,
    autofocus: false,
    hideLabel: false,
    labelText: "Title",
    initialSelectAll: false,
    editorRole: "title",
    editorDataTest: "note-title",
  }
)

const emit = defineEmits<{
  "update:modelValue": [value: string]
  blur: []
}>()

const root = ref<HTMLElement | null>(null)
const replacementWarning = ref("")
const linkWarning = ref("")

const displayWarning = computed(() => {
  const parts = [replacementWarning.value, linkWarning.value].filter(Boolean)
  return parts.join(" ")
})

function onModelUpdate(raw: string) {
  if (props.readonly) {
    replacementWarning.value = ""
    linkWarning.value = ""
    emit("update:modelValue", raw)
    return
  }
  const { value, replacementNote } = processIllegalPathChars(raw)
  replacementWarning.value = replacementNote
  linkWarning.value = hasLinkBreakChars(value) ? LINK_NAME_WARNING : ""
  emit("update:modelValue", value)
}

const seamlessBindings = computed(() => ({
  modelValue: props.modelValue,
  readonly: props.readonly,
  placeholder: props.placeholder,
  ariaLabel: props.hideLabel ? undefined : props.labelText,
  role: props.editorRole,
  "data-test": props.editorDataTest,
  "onUpdate:modelValue": onModelUpdate,
  onBlur: () => emit("blur"),
}))

onMounted(() => {
  if (!props.autofocus) return
  scheduleFocusTargetWithin(root.value, {
    selectAll: props.initialSelectAll,
  })
})
</script>

<style scoped>
.path-name-editor-join-append :deep(button) {
  height: 100%;
  min-height: 2.75rem;
}
</style>
