<template>
  <TextContentWrapper
    :value="noteDetails"
    field="edit details"
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
        :wiki-titles="wikiTitles"
        :relation-property-uses-relationship-api="
          relationPropertyUsesRelationshipApi
        "
        :relation-property-relation-type-error="
          relationPropertyRelationTypeError
        "
        @update:model-value="update(noteId, $event)"
        @blur="blur"
        @paste-complete="(content) => handlePasteComplete(content, update)"
        @dead-link-click="emit('deadLinkClick', $event)"
        @relation-property-relation-type-selected="
          onRelationPropertyRelationTypeSelected
        "
      />
    </template>
  </TextContentWrapper>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch, type PropType } from "vue"
import RichMarkdownEditor from "../../form/RichMarkdownEditor.vue"
import TextContentWrapper from "./TextContentWrapper.vue"
import TextArea from "@/components/form/TextArea.vue"
import type { WikiTitle } from "@generated/doughnut-backend-api"
import type { RelationTypeLabel } from "@/models/relationTypeOptions"
import { usePasteWithLinkImageOptions } from "@/composables/usePasteWithLinkImageOptions"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { detailsHasRelationProperty } from "@/utils/noteDetailsFrontmatter"

const emit = defineEmits<{
  deadLinkClick: [title: string]
}>()

const props = defineProps({
  noteId: { type: Number, required: true },
  noteDetails: { type: String, required: false },
  readonly: { type: Boolean, default: true },
  asMarkdown: Boolean,
  wikiTitles: { type: Array as PropType<WikiTitle[]>, required: true },
})

const relationPropertyUsesRelationshipApi = computed(() =>
  detailsHasRelationProperty(props.noteDetails ?? "")
)

const relationPropertyRelationTypeError = ref<string | undefined>()

const textareaRef = ref<InstanceType<typeof TextArea> | null>(null)
const { htmlToMarkdown, processContentAfterPaste } =
  usePasteWithLinkImageOptions()
const storageAccessor = useStorageAccessor()

watch(
  () => props.noteDetails,
  () => {
    relationPropertyRelationTypeError.value = undefined
  }
)

function onRelationPropertyRelationTypeSelected(
  relationType: RelationTypeLabel
) {
  if (!relationPropertyUsesRelationshipApi.value) return
  relationPropertyRelationTypeError.value = undefined
  storageAccessor.value
    .storedApi()
    .updateRelationship(props.noteId, { relationType: relationType })
    .catch((error) => {
      const e = error as { relationType?: string }
      if (e?.relationType) {
        relationPropertyRelationTypeError.value = e.relationType
      }
    })
}

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
