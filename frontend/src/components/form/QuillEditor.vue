<template>
  <div ref="editor"></div>
</template>

<script setup lang="ts">
import { getCurrentInstance, nextTick, ref, onMounted, watch } from "vue"
import type { Router } from "vue-router"
import Quill, { type QuillOptions, type Range } from "quill"
import "quill/dist/quill.bubble.css"
import markdownizer from "./markdownizer"

// Define soft line break blot
// Quill.import returns dynamic types that aren't fully typed in the Quill library
interface EmbedBlotInstance {
  // Blot instance interface
}
type EmbedBlotConstructor = new (
  node: Node,
  value?: unknown
) => EmbedBlotInstance

interface DeltaInstance {
  insert(content: unknown): DeltaInstance
}
type DeltaConstructor = new (ops?: unknown) => DeltaInstance

const Embed = Quill.import("blots/embed") as unknown as EmbedBlotConstructor
const BlockEmbed = Quill.import(
  "blots/block/embed"
) as unknown as EmbedBlotConstructor
const Delta = Quill.import("delta") as unknown as DeltaConstructor

class SoftLineBreakBlot extends Embed {
  static blotName = "softbreak"
  static tagName = "br"
  static className = "softbreak"
}

class HorizontalRuleBlot extends Embed {
  static blotName = "horizontalrule"
  static tagName = "hr"
}

class TableBlot extends BlockEmbed {
  static blotName = "table"
  static tagName = "table"

  static create(value: string | { html: string }) {
    // Quill.import returns dynamic types - use type assertion for static methods
    // @ts-expect-error - Quill's BlockEmbed class has static create method but types don't reflect it
    const node = super.create() as HTMLElement
    const html = typeof value === "string" ? value : value.html
    node.innerHTML = html
    node.setAttribute("contenteditable", "false")
    return node
  }

  static value(node: HTMLElement) {
    return { html: node.innerHTML }
  }
}

// Quill.register accepts dynamic blot classes - the type system can't fully validate this
Quill.register(
  SoftLineBreakBlot as unknown as Parameters<typeof Quill.register>[0],
  true
)
Quill.register(
  HorizontalRuleBlot as unknown as Parameters<typeof Quill.register>[0],
  true
)
Quill.register(
  TableBlot as unknown as Parameters<typeof Quill.register>[0],
  true
)

/** Preserves `<mark>` cloze masks from recall stems when loading HTML into Quill. */
const Inline = Quill.import("blots/inline") as typeof SoftLineBreakBlot
class MarkBlot extends Inline {
  static blotName = "mark"
  static tagName = "mark"
}
Quill.register(
  MarkBlot as unknown as Parameters<typeof Quill.register>[0],
  true
)

const props = defineProps({
  modelValue: String,
  readonly: Boolean,
})

const emits = defineEmits<{
  "update:modelValue": [value: string]
  blur: []
  pasteComplete: [content: string]
  deadLinkClick: [title: string]
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

// BR matcher for clipboard operations
const brMatcher = () => new Delta().insert({ softbreak: true })

const toolbarRows = [
  ["bold", "italic", "underline"],
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
      matchers: [["BR", brMatcher]],
      matchVisual: false,
    },
  },
  formats: [
    "bold",
    "italic",
    "underline",
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
  placeholder: props.readonly ? "" : "Enter note details here...",
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
      "click",
      (event: MouseEvent) => {
        const anchor = (event.target as HTMLElement).closest("a")
        if (!anchor) return
        event.preventDefault()
        const href = anchor.getAttribute("href")
        if (!href) return
        if (!props.readonly && anchor.classList.contains("dead-link")) {
          emits("deadLinkClick", anchor.textContent?.trim() ?? "")
          return
        }
        if (/^https?:\/\//i.test(href) || href.startsWith("//")) {
          window.open(href, "_blank", "noopener,noreferrer")
          return
        }
        router?.push(href)
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

    // Strangely, Quill does not emit a blur event when the inner editor receives a blur event
    quill.value.root.addEventListener("blur", () => {
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

<style lang="sass">
.ql-editor
  padding: 0
  margin-bottom: 15px
  &::before
    left: 0 !important
    right: 0 !important
  p
    margin: inherit !important
  pre
    margin: inherit !important
    white-space: pre-wrap
    word-wrap: break-word
  a
    cursor: pointer
  a.doughnut-link
    color: oklch(var(--a))
    text-decoration: underline
    text-decoration-style: dotted
    text-underline-offset: 0.15em
  a:not(.doughnut-link):not(.dead-link)
    color: oklch(var(--in))
    text-decoration: underline
    text-underline-offset: 0.15em
  a.dead-link
    color: red
.ql-container.ql-bubble
  border: none
  font-size: inherit !important
</style>
