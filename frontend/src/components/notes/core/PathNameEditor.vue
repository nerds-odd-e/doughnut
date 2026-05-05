<template>
  <div ref="root" class="daisy-form-control path-name-editor">
    <div v-if="$slots.append" class="daisy-join daisy-w-full path-name-editor-join">
      <div
        class="path-name-editor-join-editor daisy-join-item daisy-flex daisy-flex-1 daisy-min-w-0 daisy-items-center daisy-border daisy-border-base-content/20 daisy-bg-base-100 daisy-px-3 daisy-py-2 daisy-rounded-l-lg"
      >
        <h2 class="path-name-heading path-name-heading--inline">
          <SeamlessTextEditor
            :model-value="modelValue"
            :readonly="readonly"
            :placeholder="placeholder"
            :aria-label="hideLabel ? undefined : labelText"
            :role="editorRole"
            :data-test="editorDataTest"
            @update:model-value="onModelUpdate"
            @blur="emit('blur')"
          />
        </h2>
      </div>
      <div
        class="path-name-editor-join-append daisy-join-item daisy-flex daisy-shrink-0 daisy-self-stretch daisy-items-stretch"
      >
        <slot name="append" />
      </div>
    </div>

    <h2 v-else class="path-name-heading">
      <SeamlessTextEditor
        :model-value="modelValue"
        :readonly="readonly"
        :placeholder="placeholder"
        :aria-label="hideLabel ? undefined : labelText"
        :role="editorRole"
        :data-test="editorDataTest"
        @update:model-value="onModelUpdate"
        @blur="emit('blur')"
      />
    </h2>

    <div v-if="errorMessage" class="daisy-text-error daisy-text-sm">
      {{ errorMessage }}
    </div>
    <div v-else-if="displayWarning" class="daisy-text-warning daisy-text-sm">
      {{ displayWarning }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue"
import SeamlessTextEditor from "../../form/SeamlessTextEditor.vue"

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
h2.path-name-heading {
  font-size: 1.5rem;
  font-weight: 400;
  margin-bottom: 10px;
}

h2.path-name-heading--inline {
  margin-bottom: 0;
  width: 100%;
}

.path-name-editor-join-append :deep(button) {
  height: 100%;
  min-height: 2.75rem;
}
</style>
