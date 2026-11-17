<template>
  <template v-if="reviewedContent !== undefined">
    <p v-if="contentWithAlias === null" class="mt-2">
      `{{ answer }}` is now an alias
    </p>
    <button
      v-else
      type="button"
      class="daisy-btn daisy-btn-secondary daisy-btn-sm mt-2"
      @click="saveContent(contentWithAlias)"
    >
      Add `{{ answer }}` as an alias
    </button>
  </template>
</template>

<script setup lang="ts">
import { computed } from "vue"
import { useNoteStore } from "@/store/noteStore"
import { appendAliasToNoteContent } from "@/utils/appendAliasToNoteContent"

const props = defineProps({
  reviewedNoteId: { type: Number, required: true },
  answer: { type: String, required: true },
})

const noteStore = useNoteStore()

const reviewedContent = computed(
  () =>
    noteStore.getNoteRealmRefAndLoadWhenNeeded(props.reviewedNoteId).value?.note
      .content
)

const contentWithAlias = computed(() =>
  appendAliasToNoteContent(reviewedContent.value ?? "", props.answer)
)

function saveContent(content: string) {
  return noteStore.updateTextField(
    props.reviewedNoteId,
    "edit content",
    content
  )
}
</script>
