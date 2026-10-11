<template>
  <div
    ref="editor"
    class="seamless-editor"
    :role="role"
    :aria-labelledby="ariaLabelledby"
    :aria-label="ariaLabel"
    :data-placeholder="placeholder || undefined"
    :contenteditable="!readonly && !dictating"
    @input="onInput"
    @blur="onBlur"
    @keydown.enter.prevent="onEnter"
    @paste="onPaste"
  ></div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick } from "vue"
import type { DictationTarget } from "@/models/audio/dictationTarget"
import { dictatedInsertion } from "@/models/audio/joinDictatedSegments"

const props = defineProps({
  modelValue: { type: String, required: true },
  readonly: { type: Boolean, default: false },
  role: { type: String, required: false },
  ariaLabelledby: { type: String, required: false },
  ariaLabel: { type: String, required: false },
  placeholder: { type: String, required: false },
})

const emits = defineEmits(["update:modelValue", "blur"])
const editor = ref<HTMLElement | null>(null)
const dictating = ref(false)

function applyCaretOffsetsInSingleTextChild(
  el: HTMLElement,
  start: number,
  end: number
) {
  const tn = el.firstChild as Text | null
  if (!tn || tn.nodeType !== Node.TEXT_NODE) return
  const len = tn.length
  const a = Math.min(Math.max(0, start), len)
  const b = Math.min(Math.max(0, end), len)
  const sel = window.getSelection()
  if (!sel) return
  const range = document.createRange()
  range.setStart(tn, a)
  range.setEnd(tn, b)
  sel.removeAllRanges()
  sel.addRange(range)
}

type SelectionOffsets = { start: number; end: number }

let rememberedSelection: SelectionOffsets | null = null

function selectionOffsetsIn(el: HTMLElement): SelectionOffsets | null {
  const sel = window.getSelection()
  if (!sel?.rangeCount) return null
  const range = sel.getRangeAt(0)
  if (!el.contains(range.startContainer) || !el.contains(range.endContainer))
    return null
  const beforeRange = document.createRange()
  beforeRange.selectNodeContents(el)
  beforeRange.setEnd(range.startContainer, range.startOffset)
  const start = beforeRange.toString().length
  return { start, end: start + range.toString().length }
}

const onInput = (event: Event) => {
  const target = event.target as HTMLElement
  // Strip any HTML that might have been pasted
  const plainText = target.innerText
  emits("update:modelValue", plainText)
}

const onBlur = () => {
  if (editor.value) {
    rememberedSelection =
      selectionOffsetsIn(editor.value) ?? rememberedSelection
  }
  emits("blur")
}

const onEnter = (event: KeyboardEvent) => {
  const el = event.currentTarget as HTMLElement | null
  if (!el) return
  const form = el.closest("form")
  if (form) {
    form.requestSubmit()
    return
  }
  el.dispatchEvent(new Event("blur"))
}

const onPaste = (event: ClipboardEvent) => {
  if (props.readonly || dictating.value || !editor.value) {
    return
  }

  event.preventDefault()

  const plainText = event.clipboardData?.getData("text/plain") || ""

  if (!plainText) {
    return
  }

  focusWithSelection(replaceRange(selectionOrEnd(), () => plainText))
}

/** The selection the editor has while focused, or had when focus left it, or else the end of its text. */
const selectionOrEnd = (): SelectionOffsets => {
  const el = editor.value!
  const end = (el.innerText || "").length
  return (
    (document.activeElement === el
      ? selectionOffsetsIn(el)
      : rememberedSelection) ?? { start: end, end }
  )
}

/** Puts the composed text in place of the range and returns the caret after it. */
const replaceRange = (
  { start, end }: SelectionOffsets,
  compose: (before: string, after: string) => string
): SelectionOffsets => {
  const currentText = editor.value!.innerText || ""
  const before = currentText.substring(0, start)
  const after = currentText.substring(end)
  const inserted = compose(before, after)
  const newText = before + inserted + after
  const caret = before.length + inserted.length
  rememberedSelection = { start: caret, end: caret }

  updateContent(newText)
  emits("update:modelValue", newText)
  return rememberedSelection
}

const focusWithSelection = ({ start, end }: SelectionOffsets) =>
  nextTick(() => {
    if (editor.value) {
      editor.value.focus()
      applyCaretOffsetsInSingleTextChild(editor.value, start, end)
    }
  })

/** The selection, the end of the text, or the whole text becomes a dictation target and the editor takes no edits until it ends. */
const beginDictation = (wholeText: boolean): DictationTarget => {
  let target = wholeText
    ? { start: 0, end: (editor.value!.innerText || "").length }
    : selectionOrEnd()
  dictating.value = true
  return {
    insert: (segments) => {
      target = replaceRange(target, (before, after) =>
        dictatedInsertion(before, segments, after)
      )
    },
    end: (placeCaret) => {
      dictating.value = false
      if (placeCaret) focusWithSelection(target)
    },
  }
}

defineExpose({ beginDictation })

const updateContent = (newValue: string) => {
  if (!editor.value) return
  if (newValue === "") {
    editor.value.replaceChildren()
    return
  }
  if (editor.value.innerText === newValue) return

  const prevLen = editor.value.innerText.length
  const offsets =
    prevLen === newValue.length ? selectionOffsetsIn(editor.value) : null

  editor.value.innerText = newValue
  // Maintain single text node to prevent cursor jumping in Safari/Chrome mobile
  if (editor.value.firstChild) {
    ;(editor.value.firstChild as Text).data = newValue
  } else {
    const textNode = document.createTextNode(newValue)
    editor.value.appendChild(textNode)
  }

  if (offsets) {
    applyCaretOffsetsInSingleTextChild(editor.value, offsets.start, offsets.end)
  }
}

// Keep the editor content in sync with external changes
watch(
  () => props.modelValue,
  (newValue) => {
    if (editor.value?.innerText !== newValue) rememberedSelection = null
    updateContent(newValue)
  }
)

// Initialize content
onMounted(() => {
  if (editor.value) {
    updateContent(props.modelValue)
  }
})
</script>

<style scoped>
.seamless-editor {
  outline: none;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-width: thin;
  -ms-overflow-style: -ms-autohiding-scrollbar;
}

/* Show webkit scrollbar (Chrome, Safari, newer Edge) */
.seamless-editor::-webkit-scrollbar {
  height: 4px;
}

.seamless-editor::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.seamless-editor::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 2px;
}

.seamless-editor:empty:before {
  content: attr(data-placeholder);
  color: #888;
}
</style>
