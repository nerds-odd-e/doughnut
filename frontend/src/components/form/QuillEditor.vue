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
  donutQuillBrMatcher,
  registerDonutQuillBlots,
} from "./registerDonutQuillBlots"
import {
  handleRichContentAnchorClick,
  type DeadWikiLinkPayload,
} from "@/utils/wikiLinkMarkup"
import { DEAD_WIKI_LINK_CLASS } from "@/utils/wikiLinkDomMarkers"
import type { QuillPasteContext } from "./quillPasteContext"

registerDonutQuillBlots()

const props = defineProps({
  modelValue: String,
  readonly: Boolean,
  placeholder: { type: String, default: "Enter note content here..." },
})

const emits = defineEmits<{
  "update:modelValue": [value: string]
  blur: []
  pasteComplete: [content: string, quillContext: QuillPasteContext | null]
  deadWikiLinkClick: [payload: DeadWikiLinkPayload]
}>()

const router = getCurrentInstance()?.appContext.config.globalProperties
  .$router as Router | undefined
const editor = ref<HTMLElement | null>(null)
const quill = ref<Quill | null>(null)
const isPasting = ref(false)
const lastRange = ref<{ index: number; length: number } | null>(null)
let pendingPaste: Omit<QuillPasteContext, "insertedLength"> | null = null
let syncingModel = false

const modelHtml = () => props.modelValue || "<p><br></p>"

const onBlurTextField = () => {
  emits("blur")
}

const syncQuillFromModel = () => {
  if (!quill.value) return
  const html = modelHtml()
  if (quill.value.root.innerHTML === html) return
  syncingModel = true
  quill.value.root.innerHTML = html
  queueMicrotask(() => {
    syncingModel = false
  })
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
      matchers: [["BR", donutQuillBrMatcher]],
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
    "indent",
    "link",
    "mark",
    "softbreak",
    "horizontalrule",
    "table",
  ],
  placeholder: props.readonly ? "" : props.placeholder,
  readOnly: props.readonly,
  theme: "bubble",
}

onMounted(async () => {
  if (editor.value) {
    quill.value = new Quill(editor.value, options)

    syncQuillFromModel()

    await nextTick()

    if (!props.readonly && quill.value) {
      quill.value.root.addEventListener(
        "paste",
        (event: ClipboardEvent) => {
          if (!event.clipboardData) return

          const originalGetData = event.clipboardData.getData.bind(
            event.clipboardData
          )

          // Quill's own getSelection() can throw when the browser's native
          // selection doesn't map onto a blot (e.g. no real caret was ever
          // placed); when that happens there is simply no paste context to
          // capture, matching the existing insertTextAtCursor precedent below.
          let range: Range | null = null
          try {
            range = quill.value?.getSelection(true) ?? null
          } catch {
            range = null
          }
          pendingPaste = range
            ? {
                originalText: originalGetData("text/plain"),
                range: { index: range.index, length: range.length },
              }
            : null

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
        const anchor = (event.target as HTMLElement).closest(
          `a.${DEAD_WIKI_LINK_CLASS}`
        )
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
            onDeadWikiLink: (payload) => emits("deadWikiLinkClick", payload),
            navigateInApp: (to) => {
              router?.push(to)
            },
          },
          { deadWikiLinksEnabled: !props.readonly }
        )
      },
      true
    )

    quill.value.on("text-change", (delta) => {
      const content = quill.value!.root.innerHTML
      if (!syncingModel && content !== modelHtml()) {
        emits("update:modelValue", content)
      }
      if (isPasting.value) {
        isPasting.value = false
        const context: QuillPasteContext | null = pendingPaste
          ? {
              ...pendingPaste,
              insertedLength:
                delta.length() -
                pendingPaste.range.index -
                pendingPaste.range.length,
            }
          : null
        pendingPaste = null
        emits("pasteComplete", content, context)
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

watch(() => props.modelValue, syncQuillFromModel)

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
