<template>
  <div
    v-if="step.kind === 'list'"
    class="flex flex-col gap-3"
    data-testid="accidental-match-resolve-dialog"
  >
    <p
      class="text-sm opacity-80"
      data-testid="resolve-overlap-explanation"
    >
      Overlap is for a note that largely overlaps with the current note. It may
      be a technically correct answer, but we expect a more precise answer here.
    </p>
    <ul class="flex flex-col gap-2">
      <AccidentalMatchResolveRow
        v-for="matched in matchedNotes"
        :key="matched.id"
        :matched="matched"
        :can-mutate="canOfferMutatingAction(matched.id)"
        :add-as-overlapped-disabled="isOverlapAlreadyDeclared(matched.id)"
        @add-wiki-link-or-relationship="openWikiLinkOrRelationshipOffer(matched.id)"
        @add-as-overlapped="addAsOverlappedNote(matched.id)"
      />
    </ul>
  </div>
  <MatchedNoteWikiLinkOrRelationshipOffer
    v-else
    :reviewed-note-id="reviewedNoteId"
    :matched-note-id="step.matchedNoteId"
    @close-dialog="returnToList"
  />
</template>

<script setup lang="ts">
import { computed, inject, ref, type PropType, type Ref } from "vue"
import type { NoteTopology, User } from "@generated/donut-backend-api"
import AccidentalMatchResolveRow from "@/components/recall/AccidentalMatchResolveRow.vue"
import MatchedNoteWikiLinkOrRelationshipOffer from "@/components/recall/MatchedNoteWikiLinkOrRelationshipOffer.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { appendOverlapWikiLinkToNoteContent } from "@/utils/appendOverlapWikiLinkToNoteContent"
import { buildWikiLinkText } from "@/utils/buildWikiLinkText"
import { noteContentDeclaresOverlapWikiLink } from "@/utils/overlapWikiLinkTokens"

const props = defineProps({
  matchedNotes: {
    type: Array as PropType<NoteTopology[]>,
    required: true,
  },
  reviewedNoteId: {
    type: Number,
    required: true,
  },
})

type ResolveStep =
  | { kind: "list" }
  | { kind: "wiki-link-or-relationship"; matchedNoteId: number }

const step = ref<ResolveStep>({ kind: "list" })

const currentUser = inject<Ref<User | undefined>>("currentUser")
const storageAccessor = useStorageAccessor()

const reviewedRealm = computed(
  () =>
    storageAccessor.value
      .storedApi()
      .getNoteRealmRefAndLoadWhenNeeded(props.reviewedNoteId).value
)

function canOfferMutatingAction(matchedNoteId: number): boolean {
  if (!currentUser?.value || !reviewedRealm.value) return false
  if (reviewedRealm.value.notebookRealm.readonly === true) return false
  const matchedRealm = storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndLoadWhenNeeded(matchedNoteId).value
  return !!matchedRealm
}

function overlapAppendContext(matchedNoteId: number) {
  const reviewed = reviewedRealm.value
  const matched = storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndLoadWhenNeeded(matchedNoteId).value
  if (!reviewed?.note || !matched) return null
  return {
    content: reviewed.note.content ?? "",
    target: {
      noteTopology: matched.note.noteTopology,
      notebookId: matched.notebookRealm.notebook.id,
      notebookName: matched.notebookRealm.notebook.name,
    },
    source: { notebookId: reviewed.notebookRealm.notebook.id },
  }
}

function isOverlapAlreadyDeclared(matchedNoteId: number): boolean {
  const ctx = overlapAppendContext(matchedNoteId)
  if (!ctx) return false
  return noteContentDeclaresOverlapWikiLink(
    ctx.content,
    buildWikiLinkText(ctx.target, ctx.source)
  )
}

function openWikiLinkOrRelationshipOffer(matchedNoteId: number) {
  step.value = { kind: "wiki-link-or-relationship", matchedNoteId }
}

function returnToList() {
  step.value = { kind: "list" }
}

async function addAsOverlappedNote(matchedNoteId: number) {
  const ctx = overlapAppendContext(matchedNoteId)
  if (!ctx) return

  const composed = appendOverlapWikiLinkToNoteContent(
    ctx.content,
    ctx.target,
    ctx.source
  )
  if (composed === null) return

  await storageAccessor.value
    .storedApi()
    .updateTextField(props.reviewedNoteId, "edit content", composed)
}
</script>
