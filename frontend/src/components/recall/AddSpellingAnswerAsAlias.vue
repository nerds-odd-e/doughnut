<template>
  <p v-if="aliasesHoldAnswer" class="mt-2">`{{ answer }}` is now an alias</p>
  <button
    v-else-if="contentWithAlias !== null"
    type="button"
    class="daisy-btn daisy-btn-secondary daisy-btn-sm mt-2"
    @click="saveContent(contentWithAlias)"
  >
    Add `{{ answer }}` as an alias
  </button>
</template>

<script setup lang="ts">
import { computed } from "vue"
import { useNoteStore } from "@/store/noteStore"
import {
  appendAliasToNoteContent,
  noteContentHasAlias,
} from "@/utils/appendAliasToNoteContent"
import { isValidPlainAliasText } from "@/utils/authoredAliasesValidation"

const props = defineProps({
  reviewedNoteId: { type: Number, required: true },
  answer: { type: String, required: true },
})

const noteStore = useNoteStore()

const editableContent = computed(() => {
  const realm = noteStore.getNoteRealmRefAndLoadWhenNeeded(
    props.reviewedNoteId
  ).value
  if (!realm || realm.notebookRealm.readonly === true) return
  if (props.answer === "" || !isValidPlainAliasText(props.answer)) return
  return realm.note.content ?? ""
})

const aliasesHoldAnswer = computed(
  () =>
    editableContent.value !== undefined &&
    noteContentHasAlias(editableContent.value, props.answer)
)

const contentWithAlias = computed(() =>
  editableContent.value === undefined
    ? null
    : appendAliasToNoteContent(editableContent.value, props.answer)
)

function saveContent(content: string) {
  return noteStore.updateTextField(
    props.reviewedNoteId,
    "edit content",
    content
  )
}
</script>
