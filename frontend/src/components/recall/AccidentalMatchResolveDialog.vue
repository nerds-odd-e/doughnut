<template>
  <ul
    v-if="step.kind === 'list'"
    class="flex flex-col gap-2"
    data-testid="accidental-match-resolve-dialog"
  >
    <AccidentalMatchResolveRow
      v-for="matched in matchedNotes"
      :key="matched.id"
      :matched="matched"
      :can-build-link="canOfferBuildLink(matched.id)"
      @build-link="openLinkOffer(matched.id)"
    />
  </ul>
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

function canOfferBuildLink(matchedNoteId: number): boolean {
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
</script>
