<template>
  <WikiLinkOrRelationshipChoice
    v-if="selectedSearchResult && sourceNote && !targetSearchResult"
    :target-note-topology="selectedSearchResult.noteTopology"
    :bare-wiki-link-available="false"
    :insert-wiki-link-as-property-available="insertWikiLinkAsPropertyAvailable"
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
import type { Note, NoteSearchResult } from "@generated/donut-backend-api"
import AddRelationshipFinalize from "@/components/wiki-link-or-relationship/AddRelationshipFinalize.vue"
import WikiLinkOrRelationshipChoice from "@/components/wiki-link-or-relationship/WikiLinkOrRelationshipChoice.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { parseNoteContentMarkdown } from "@/utils/noteContentFrontmatterParse"
import { appendWikiLinkPropertyRow } from "@/utils/noteContentPropertyRows"
import { authoredWikiLinkTokenForInsert } from "@/utils/wikiLinkAuthoring"

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

const insertWikiLinkAsPropertyAvailable = computed(
  () => parseNoteContentMarkdown(sourceNote.value?.content ?? "").ok
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
  const linkText = await authoredWikiLinkTokenForInsert(
    source.id,
    target.noteTopology.id
  )
  if (linkText === undefined) return
  const composed = appendWikiLinkPropertyRow(source.content ?? "", linkText)
  if (composed === undefined) return
  await closeDialogThen(() =>
    storageAccessor.value
      .storedApi()
      .updateTextField(source.id, "edit content", composed)
  )
}
</script>
