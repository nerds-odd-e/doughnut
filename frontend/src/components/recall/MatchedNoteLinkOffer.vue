<template>
  <LinkInsertionChoice
    v-if="selectedSearchResult && sourceNote && !targetSearchResult"
    :target-note-topology="selectedSearchResult.noteTopology"
    :bare-wiki-link-available="false"
    :wiki-property-option-available="wikiPropertyOptionAvailable"
    @choose-insert-wiki-link-as-property="onInsertWikiLinkAsProperty"
    @choose-add-relationship="chooseAddRelationship"
    @go-back="$emit('closeDialog')"
  />
  <AddRelationshipFinalize
    v-if="targetSearchResult && sourceNote"
    :note="sourceNote"
    :target-search-result="targetSearchResult"
    :navigate-on-success="false"
    @success="$emit('closeDialog')"
    @go-back="targetSearchResult = undefined"
  />
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from "vue"
import type { Note, NoteSearchResult } from "@generated/doughnut-backend-api"
import AddRelationshipFinalize from "@/components/links/AddRelationshipFinalize.vue"
import LinkInsertionChoice from "@/components/links/LinkInsertionChoice.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { buildWikiLinkText } from "@/utils/buildWikiLinkText"
import { parseNoteContentMarkdown } from "@/utils/noteContentFrontmatterParse"
import { appendWikiLinkPropertyRow } from "@/utils/noteContentPropertyRows"

const props = defineProps<{
  reviewedNoteId: number
  matchedNoteId: number
}>()

const emit = defineEmits<{
  closeDialog: []
}>()

const storageAccessor = useStorageAccessor()

const reviewedRealmRef = storageAccessor.value
  .storedApi()
  .getNoteRealmRefAndLoadWhenNeeded(props.reviewedNoteId)
const matchedRealmRef = storageAccessor.value
  .storedApi()
  .getNoteRealmRefAndLoadWhenNeeded(props.matchedNoteId)

const sourceNote = computed<Note | undefined>(
  () => reviewedRealmRef.value?.note
)

const selectedSearchResult = computed<NoteSearchResult | undefined>(() => {
  const realm = matchedRealmRef.value
  if (!realm) return undefined
  return {
    noteTopology: realm.note.noteTopology,
    notebookId: realm.notebookRealm.notebook.id,
    notebookName: realm.notebookRealm.notebook.name,
  }
})

const targetSearchResult = ref<NoteSearchResult | undefined>(undefined)

const wikiPropertyOptionAvailable = computed(
  () => parseNoteContentMarkdown(sourceNote.value?.content ?? "").ok
)

const sourceNotebookId = computed(
  () => reviewedRealmRef.value?.notebookRealm.notebook.id
)

function chooseAddRelationship() {
  targetSearchResult.value = selectedSearchResult.value
}

async function closeDialogThen(run: () => void | Promise<void>) {
  emit("closeDialog")
  await nextTick()
  await run()
}

async function onInsertWikiLinkAsProperty() {
  const source = sourceNote.value
  const target = selectedSearchResult.value
  if (!source || !target) return
  const linkText = buildWikiLinkText(target, {
    notebookId: sourceNotebookId.value,
  })
  const composed = appendWikiLinkPropertyRow(source.content ?? "", linkText)
  if (composed === undefined) return
  await closeDialogThen(() =>
    storageAccessor.value
      .storedApi()
      .updateTextField(source.id, "edit content", composed)
  )
}
</script>
