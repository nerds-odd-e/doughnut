<template>
  <TextContentWrapper
    :value="noteDetails"
    field="edit details"
    @details-saved="$emit('details-saved', $event)"
  >
    <template #default="{ value, update, blur }">
      <TextArea
        v-if="asMarkdown"
        ref="textareaRef"
        :multiple-line="true"
        scope-name="note"
        :model-value="value"
        :readonly="readonly"
        :auto-extend-until="1000"
        @update:model-value="update(noteId, $event)"
        @blur="blur"
        @paste="(event) => handleTextareaPaste(event, value, update)"
      />
      <RichMarkdownEditor
        v-else
        :multiple-line="true"
        scope-name="note"
        :model-value="value"
        :readonly="readonly"
        @update:model-value="update(noteId, $event)"
        @blur="blur"
        @paste-complete="(content) => handlePasteComplete(content, update)"
      />
    </template>
  </TextContentWrapper>
</template>

<script setup lang="ts">
import { nextTick, ref } from "vue"
import RichMarkdownEditor from "../../form/RichMarkdownEditor.vue"
import TextContentWrapper from "./TextContentWrapper.vue"
import TextArea from "@/components/form/TextArea.vue"
import { usePasteWithLinkImageOptions } from "@/composables/usePasteWithLinkImageOptions"

const props = defineProps({
  noteId: { type: Number, required: true },
  noteDetails: { type: String, required: false },
  readonly: { type: Boolean, default: true },
  asMarkdown: Boolean,
})

defineEmits<{
  (e: "details-saved", newValue: string): void
}>()

const textareaRef = ref<InstanceType<typeof TextArea> | null>(null)
const { htmlToMarkdown, processContentAfterPaste } =
  usePasteWithLinkImageOptions()

const offerToRemoveLinksAndImages = async (
  content: string,
  update: (noteId: number, newValue: string) => void
) => {
  const processedContent = await processContentAfterPaste(content)
  if (processedContent !== null) {
    update(props.noteId, processedContent)
  }
}

const handleTextareaPaste = async (
  event: ClipboardEvent,
  currentValue: string | undefined,
  update: (noteId: number, newValue: string) => void
) => {
  if (!props.asMarkdown || !textareaRef.value) return

  const htmlData = event.clipboardData?.getData("text/html")
  if (!htmlData) return

  event.preventDefault()

  const textarea = textareaRef.value?.$el?.querySelector(
    "textarea"
  ) as HTMLTextAreaElement | null
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const markdown = htmlToMarkdown(htmlData)
  const newValue =
    (currentValue || "").slice(0, start) +
    markdown +
    (currentValue || "").slice(end)

  update(props.noteId, newValue)
  nextTick(() => {
    if (textarea) {
      textarea.selectionStart = textarea.selectionEnd = start + markdown.length
    }
  })

  await offerToRemoveLinksAndImages(newValue, update)
}

const handlePasteComplete = async (
  currentValue: string | undefined,
  update: (noteId: number, newValue: string) => void
) => {
  if (!currentValue) return
  await offerToRemoveLinksAndImages(currentValue, update)
}
</script>
