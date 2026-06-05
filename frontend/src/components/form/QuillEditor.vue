<template>
  <div ref="editor"></div>
</template>

<script setup lang="ts">
import { getCurrentInstance, nextTick, ref, onMounted, watch } from "vue"
import type { Router } from "vue-router"
import Quill, { type QuillOptions, type Range } from "quill"
import "quill/dist/quill.bubble.css"
import markdownizer from "./markdownizer"
import {
  doughnutQuillBrMatcher,
  registerDoughnutQuillBlots,
} from "./registerDoughnutQuillBlots"
import {
  handleRichContentAnchorClick,
  type DeadLinkPayload,
} from "@/utils/wikiPropertyValueField"

registerDoughnutQuillBlots()

const props = defineProps({
  modelValue: String,
  readonly: Boolean,
})

const emits = defineEmits<{
  "update:modelValue": [value: string]
  blur: []
  pasteComplete: [content: string]
  deadLinkClick: [payload: DeadLinkPayload]
}>()

const router = getCurrentInstance()?.appContext.config.globalProperties
  .$router as Router | undefined
const localValue = ref(props.modelValue)
const editor = ref<HTMLElement | null>(null)
const quill = ref<Quill | null>(null)
const isPasting = ref(false)
const lastRange = ref<{ index: number; length: number } | null>(null)

const onBlurTextField = () => {
  emits("blur")
}

const updateQuillContent = (content: string | undefined) => {
  if (quill.value) {
    quill.value.root.innerHTML = content ?? ""
  }
}

// Shift+Enter handler for soft line breaks
const shiftEnterHandler = function (
  this: { quill: Quill },
  range: Range | null
) {
  if (!range) return
  this.quill.insertEmbed(range.index, "softbreak", true, Quill.sources.USER)
  this.quill.insertText(range.index + 1, "\u200B", Quill.sources.USER)
  this.quill.setSelection(range.index + 1, Quill.sources.SILENT)
}

const toolbarRows = [
  ["bold", "italic", "underline", "code"],
  [{ header: 1 }, { header: 2 }],
  ["blockquote", "code-block"],
  [{ list: "ordered" }, { list: "bullet" }],
  ["link"],
]

const options: QuillOptions = {
  modules: {
    toolbar: props.readonly ? false : toolbarRows,
    keyboard: {
      bindings: {
        shiftEnter: {
          key: "Enter",
          shiftKey: true,
          handler: shiftEnterHandler,
        },
      },
    },
    clipboard: {
      matchers: [["BR", doughnutQuillBrMatcher]],
      matchVisual: false,
    },
  },
  formats: [
    "bold",
    "italic",
    "underline",
    "code",
    "header",
    "blockquote",
    "code-block",
    "list",
    "link",
    "mark",
    "softbreak",
    "horizontalrule",
    "table",
  ],
  placeholder: props.readonly ? "" : "Enter note content here...",
  readOnly: props.readonly,
  theme: "bubble",
}

onMounted(async () => {
  if (editor.value) {
    quill.value = new Quill(editor.value, options)

    // Set initial content
    updateQuillContent(localValue.value)

    // Wait for next tick to ensure Quill is fully initialized
    await nextTick()

    if (!props.readonly && quill.value) {
      quill.value.root.addEventListener(
        "paste",
        (event: ClipboardEvent) => {
          if (!event.clipboardData) return

          const originalGetData = event.clipboardData.getData.bind(
            event.clipboardData
          )

          event.clipboardData.getData = (format: string) => {
            if (format === "text/html") {
              const htmlData = originalGetData(format)
              if (htmlData) {
                const markdown = markdownizer.htmlToMarkdown(htmlData)
                return markdownizer.markdownToHtml(markdown, {
                  preserve_pre: true,
                })
              }
            }
            return originalGetData(format)
          }

          // Mark paste in progress; emit after Quill updates content
          isPasting.value = true
        },
        true
      )
    }

    quill.value.root.addEventListener(
      "mousedown",
      (event: MouseEvent) => {
        if (props.readonly) return
        const anchor = (event.target as HTMLElement).closest("a.dead-link")
        if (anchor) event.preventDefault()
      },
      true
    )

    quill.value.root.addEventListener(
      "click",
      (event: MouseEvent) => {
        const anchor = (event.target as HTMLElement).closest("a")
        if (!anchor) return
        event.preventDefault()
        handleRichContentAnchorClick(
          anchor,
          {
            onDeadLink: (payload) => emits("deadLinkClick", payload),
            navigateInApp: (href) => {
              router?.push(href)
            },
          },
          { deadLinksEnabled: !props.readonly }
        )
      },
      true
    )

    // Listen for text changes
    quill.value.on("text-change", () => {
      const content = quill.value!.root.innerHTML
      localValue.value = content
      onUpdateContent()
      if (isPasting.value) {
        isPasting.value = false
        emits("pasteComplete", content)
      }
    })

    quill.value.on("selection-change", (range) => {
      if (!range) {
        onBlurTextField()
      } else {
        lastRange.value = { index: range.index, length: range.length }
      }
    })

    // Forward DOM blur to Quill only when focus leaves the editor (not to a link inside it).
    quill.value.root.addEventListener("blur", (event: FocusEvent) => {
      const related = event.relatedTarget
      if (related instanceof Node && quill.value?.root.contains(related)) {
        return
      }
      quill.value?.blur()
    })
  }
})

// Watch for changes in modelValue prop
watch(
  () => props.modelValue,
  (newValue) => {
    if (quill.value && localValue.value !== newValue) {
      localValue.value = newValue
      updateQuillContent(newValue)
    }
  }
)

const onUpdateContent = () => {
  emits("update:modelValue", localValue.value ?? "")
}

function insertTextAtCursor(text: string) {
  if (!quill.value) return
  if (lastRange.value === null) {
    // Editor had no cursor (e.g. note was in readonly/view mode).
    // Fall through to the caller's insertMarkdownAtEnd path.
    return false
  }
  const index = lastRange.value.index
  // Tell the caller that we handled it
  quill.value.insertText(index, text, Quill.sources.USER)
  try {
    quill.value.setSelection(index + text.length, 0, Quill.sources.SILENT)
  } catch {
    // ignore if editor DOM is not ready
  }
  return true
}

defineExpose({ insertTextAtCursor })
</script>
