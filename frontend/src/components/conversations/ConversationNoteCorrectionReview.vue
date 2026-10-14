<template>
  <section class="daisy-card bg-base-100 p-4 mt-2" data-testid="conversation-note-correction">
    <h3>Review {{ correction.field === 'TITLE' ? 'title' : 'content' }} correction</h3>
    <p>{{ correction.note?.noteTopology.title }}</p>
    <p class="daisy-badge">{{ correction.decision === 'PENDING' ? 'Pending review' : correction.decision === 'DECLINED' ? 'Declined' : 'Accepted' }}</p>
    <div :class="RICH_CONTENT_PROSE" v-html="markdownizer.markdownToHtml(correction.reason)" />
    <NoteRealmLoader v-if="correction.note" :note-id="correction.note.id">
      <template #default="{ noteRealm }">
        <ContentLoader v-if="!noteRealm" />
        <template v-else>
          <h4>Current {{ correction.field === 'TITLE' ? 'title' : 'content' }}</h4>
          <p v-if="correction.field === 'TITLE'">{{ noteRealm.note.noteTopology.title }}</p>
          <div v-else :class="RICH_CONTENT_PROSE" v-html="markdownizer.markdownToHtml(noteRealm.note.content)" />
          <template v-if="correction.decision === 'PENDING' && user && !noteRealm.notebookRealm.readonly">
            <details>
              <summary>Original proposal</summary>
              <p v-if="correction.field === 'TITLE'">{{ correction.proposedValue }}</p>
              <div v-else :class="RICH_CONTENT_PROSE" v-html="markdownizer.markdownToHtml(correction.proposedValue)" />
            </details>
            <TextInput v-if="correction.field === 'TITLE'" v-model="reviewedValue" :scope-name="`correction-${correction.id}`" field="proposedTitle" />
            <TextArea v-else v-model="reviewedValue" :scope-name="`correction-${correction.id}`" field="proposedContent" />
            <label v-if="requiresReferenceHandling(noteRealm)" class="block mt-2">
              {{ TITLE_RENAME_REFERENCE_PROMPT }}
              <select v-model="referenceHandling" class="daisy-select block" aria-label="Wiki reference handling">
                <option value="">Choose reference handling</option>
                <option v-for="option in TITLE_RENAME_REFERENCE_SAVE_OPTIONS" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
            </label>
            <button v-if="staleReview" class="daisy-btn daisy-btn-outline mt-2" :disabled="deciding || refreshingReview" @click="loadCurrentReview">Load current content</button>
            <button class="daisy-btn daisy-btn-primary mt-2" :disabled="deciding || refreshingReview || (requiresReferenceHandling(noteRealm) && !referenceHandling)" @click="accept">Accept</button>
          </template>
          <template v-else>
            <h4>Proposed {{ correction.field === 'TITLE' ? 'title' : 'content' }}</h4>
            <p v-if="correction.field === 'TITLE'">{{ correction.proposedValue }}</p>
            <div v-else :class="RICH_CONTENT_PROSE" v-html="markdownizer.markdownToHtml(correction.proposedValue)" />
          </template>
        </template>
      </template>
    </NoteRealmLoader>
    <template v-if="correction.decision === 'ACCEPTED'">
      <h4>Reviewed {{ correction.field === 'TITLE' ? 'title' : 'content' }}</h4>
      <p v-if="correction.field === 'TITLE'">{{ correction.reviewedValue }}</p>
      <div v-else data-testid="accepted-reviewed-content" :class="RICH_CONTENT_PROSE" v-html="markdownizer.markdownToHtml(correction.reviewedValue)" />
      <p>Accepted revision: {{ correction.acceptedRevision }}</p>
      <button v-if="correction.note" class="daisy-btn daisy-btn-outline mt-2" @click="openNote">Open note</button>
    </template>
    <button v-if="user && correction.decision === 'PENDING'" class="daisy-btn daisy-btn-outline mt-2" :disabled="deciding" @click="decline">Decline</button>
    <p v-if="reviewRefreshFailure" class="text-error">{{ reviewRefreshFailure }}</p>
    <p v-if="decisionFailure" class="text-error">{{ decisionFailure }}</p>
    <p v-if="noteRefreshFailed" class="text-error">The correction is accepted. Could not reload the note. Open the note to try again.</p>
  </section>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { useRouter } from "vue-router"
import type {
  ConversationNoteCorrection,
  NoteRealm,
  User,
} from "@generated/donut-backend-api"
import { ConversationMessageController } from "@generated/donut-backend-api/sdk.gen"
import {
  TITLE_RENAME_REFERENCE_PROMPT,
  TITLE_RENAME_REFERENCE_SAVE_OPTIONS,
} from "@/components/notes/core/titleRenameReferenceChoices"
import { toOpenApiError } from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import NoteRealmLoader from "@/components/notes/NoteRealmLoader.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import TextInput from "@/components/form/TextInput.vue"
import TextArea from "@/components/form/TextArea.vue"
import markdownizer from "@/components/form/markdownizer"
import { RICH_CONTENT_PROSE } from "@/constants/richContentProse"
import {
  useNoteStore,
  type TitleRenameReferenceHandling,
} from "@/store/noteStore"
import { noteShowLocation } from "@/routes/noteShowLocation"

const props = defineProps<{
  correction: ConversationNoteCorrection
  user: User
}>()
const emit = defineEmits<{
  (e: "decided", correction: ConversationNoteCorrection): void
  (e: "open-note"): void
}>()
const router = useRouter()
const noteStore = useNoteStore()
const reviewedValue = ref(props.correction.proposedValue ?? "")
const reviewedBase = ref(props.correction.baseRevision)
const staleReview = ref(false)
const refreshingReview = ref(false)
const reviewRefreshFailure = ref("")
const referenceHandling = ref<TitleRenameReferenceHandling | "">("")
const deciding = ref(false)
const decisionFailure = ref("")
const noteRefreshFailed = ref(false)

const requiresReferenceHandling = (noteRealm: NoteRealm) =>
  props.correction.field === "TITLE" &&
  reviewedValue.value !== noteRealm.note.noteTopology.title &&
  (noteRealm.references?.length ?? 0) > 0

const refreshNote = async () => {
  try {
    await noteStore.loadNoteRealm(props.correction.note!.id)
    noteRefreshFailed.value = false
  } catch {
    noteRefreshFailed.value = true
  }
}

const loadCurrentReview = async () => {
  refreshingReview.value = true
  const { data: currentReview, error } = await apiCallWithLoading(() =>
    ConversationMessageController.loadCurrentReview({
      path: { correctionId: props.correction.id },
    })
  )
  refreshingReview.value = false
  reviewRefreshFailure.value = error
    ? "Could not load the current note. Your review is kept. Try again."
    : ""
  if (!error) {
    noteStore.refreshNoteRealm(currentReview!.noteRealm!)
    reviewedBase.value = currentReview!.baseRevision
    staleReview.value = false
    decisionFailure.value =
      "Current note loaded. Review your wording, then choose Accept."
  }
}

const accept = async () => {
  deciding.value = true
  const {
    data: correction,
    error,
    response,
  } = await apiCallWithLoading(() =>
    ConversationMessageController.acceptCorrection({
      path: { correctionId: props.correction.id },
      body: {
        reviewedValue: reviewedValue.value,
        baseRevision: reviewedBase.value,
        ...(referenceHandling.value
          ? { referenceHandling: referenceHandling.value }
          : {}),
      },
    })
  )
  deciding.value = false
  const errors = error ? toOpenApiError(error).errors : undefined
  staleReview.value = !!error && response?.status === 409
  decisionFailure.value = staleReview.value
    ? "This review is stale. Load the current note and review again."
    : error
      ? errors?.newTitle ||
        errors?.referenceHandling ||
        "Could not confirm acceptance. Try again to recover this review's decision."
      : ""
  if (!error) {
    emit("decided", correction!)
    if (correction!.decision === "ACCEPTED") await refreshNote()
  }
}

const openNote = async () => {
  await refreshNote()
  await router.push(noteShowLocation(props.correction.note!.id))
  emit("open-note")
}

const decline = async () => {
  deciding.value = true
  const { data: correction, error } = await apiCallWithLoading(() =>
    ConversationMessageController.declineCorrection({
      path: { correctionId: props.correction.id! },
    })
  )
  deciding.value = false
  decisionFailure.value = error ? "Could not decline. Try again." : ""
  if (!error) emit("decided", correction!)
}
</script>
