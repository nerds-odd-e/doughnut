<template>
  <div
    class="daisy-alert"
    :class="{
      'daisy-alert-success': answeredQuestion.answer.correct,
      'daisy-alert-error': !answeredQuestion.answer.correct,
    }"
    :data-testid="
      answeredQuestion.answer.outcome === 'ACCIDENTAL_MATCH'
        ? 'accidental-match-alert'
        : undefined
    "
  >
    <strong>{{ alertMessage }}</strong>
  </div>
  <NoteUnderQuestion
    v-bind="recalledNoteUnderQuestionProps(answeredQuestion.recalledNote)"
  />
  <ViewMemoryTrackerLink
    :memory-tracker-id="answeredQuestion.memoryTrackerId"
  />
  <NoteShow
    :note-id="answeredQuestion.recalledNote.noteTopology.id"
    :expand-children="false"
  />
  <section
    v-if="showMatchedNotesSection"
    class="mt-6"
    data-testid="matched-notes-section"
  >
    <h4 class="text-lg font-semibold mb-4 text-base-content">Matched note(s)</h4>
    <div class="flex flex-col gap-4">
      <div
        v-for="matched in answeredQuestion.matchedNotes"
        :key="matched.id"
        :data-testid="`matched-note-${matched.id}`"
      >
        <NoteShow :note-id="matched.id" :expand-children="false" />
        <PopButton
          v-if="canOfferLinkToMatched(matched.id)"
          :title="'Link to this note'"
          :aria-label="'Link to this note'"
          btn-class="daisy-btn daisy-btn-secondary daisy-btn-sm mt-2"
          :data-testid="`link-to-matched-note-${matched.id}`"
          :show-close-button="false"
        >
          <template #default="{ closer }">
            <MatchedNoteLinkOffer
              :reviewed-note-id="reviewedNoteId"
              :matched-note-id="matched.id"
              @close-dialog="closer"
            />
          </template>
        </PopButton>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, inject, type PropType, type Ref } from "vue"
import type { AnsweredQuestion, User } from "@generated/doughnut-backend-api"
import NoteShow from "@/components/notes/NoteShow.vue"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import MatchedNoteLinkOffer from "@/components/recall/MatchedNoteLinkOffer.vue"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"
import ViewMemoryTrackerLink from "./ViewMemoryTrackerLink.vue"
import { recalledNoteUnderQuestionProps } from "./recalledNoteUnderQuestionProps"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const props = defineProps({
  answeredQuestion: {
    type: Object as PropType<AnsweredQuestion>,
    required: true,
  },
})

const currentUser = inject<Ref<User | undefined>>("currentUser")
const storageAccessor = useStorageAccessor()

const reviewedNoteId = computed(
  () => props.answeredQuestion.recalledNote.noteTopology.id
)

const reviewedRealm = computed(
  () =>
    storageAccessor.value
      .storedApi()
      .getNoteRealmRefAndLoadWhenNeeded(reviewedNoteId.value).value
)

const showMatchedNotesSection = computed(
  () =>
    props.answeredQuestion.answer.outcome === "ACCIDENTAL_MATCH" &&
    (props.answeredQuestion.matchedNotes?.length ?? 0) > 0
)

function canOfferLinkToMatched(matchedNoteId: number): boolean {
  if (!currentUser?.value || !reviewedRealm.value) return false
  if (reviewedRealm.value.notebookRealm.readonly === true) return false
  const matchedRealm = storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndLoadWhenNeeded(matchedNoteId).value
  return !!matchedRealm
}

const alertMessage = computed(() => {
  const { answer } = props.answeredQuestion
  if (answer.outcome === "ACCIDENTAL_MATCH") {
    return `Your answer \`${answer.spellingAnswer}\` names another note — not correct for this review.`
  }
  if (answer.correct) {
    return "Correct!"
  }
  return `Your answer \`${answer.spellingAnswer}\` is incorrect.`
})
</script>
