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
        @build-link="openLinkOffer(matched.id)"
        @add-as-overlapped="addAsOverlappedNote(matched.id)"
      />
    </ul>
  </div>
  <MatchedNoteLinkOffer
    v-else
    :reviewed-note-id="reviewedNoteId"
    :matched-note-id="step.matchedNoteId"
    @close-dialog="returnToList"
  />
</template>

<script setup lang="ts">
import { computed, inject, ref, type PropType, type Ref } from "vue"
import type { NoteTopology, User } from "@generated/doughnut-backend-api"
import AccidentalMatchResolveRow from "@/components/recall/AccidentalMatchResolveRow.vue"
import MatchedNoteLinkOffer from "@/components/recall/MatchedNoteLinkOffer.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { appendOverlapWikiLinkToNoteContent } from "@/utils/appendOverlapWikiLinkToNoteContent"

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

type ResolveStep = { kind: "list" } | { kind: "link"; matchedNoteId: number }

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

function openLinkOffer(matchedNoteId: number) {
  step.value = { kind: "link", matchedNoteId }
}

function returnToList() {
  step.value = { kind: "list" }
}

async function addAsOverlappedNote(matchedNoteId: number) {
  const reviewed = reviewedRealm.value
  const matched = storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndLoadWhenNeeded(matchedNoteId).value
  if (!reviewed?.note || !matched) return

  const composed = appendOverlapWikiLinkToNoteContent(
    reviewed.note.content ?? "",
    {
      noteTopology: matched.note.noteTopology,
      notebookId: matched.notebookRealm.notebook.id,
      notebookName: matched.notebookRealm.notebook.name,
    },
    { notebookId: reviewed.notebookRealm.notebook.id }
  )
  if (composed === null) return

  await storageAccessor.value
    .storedApi()
    .updateTextField(props.reviewedNoteId, "edit content", composed)
}
</script>
