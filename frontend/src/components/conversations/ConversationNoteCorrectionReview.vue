<template>
  <section class="daisy-card bg-base-100 p-4 mt-2" data-testid="conversation-note-correction">
    <h3>Review {{ correction.field === 'TITLE' ? 'title' : 'content' }} correction</h3>
    <p>{{ correction.note?.noteTopology.title }}</p>
    <p class="daisy-badge">Pending review</p>
    <div :class="RICH_CONTENT_PROSE" v-html="markdownizer.markdownToHtml(correction.reason)" />
    <NoteRealmLoader v-if="correction.note" :note-id="correction.note.id">
      <template #default="{ noteRealm }">
        <ContentLoader v-if="!noteRealm" />
        <template v-else>
          <h4>Current {{ correction.field === 'TITLE' ? 'title' : 'content' }}</h4>
          <p v-if="correction.field === 'TITLE'">{{ noteRealm.note.noteTopology.title }}</p>
          <div v-else :class="RICH_CONTENT_PROSE" v-html="markdownizer.markdownToHtml(noteRealm.note.content)" />
          <template v-if="user && !noteRealm.notebookRealm.readonly">
            <TextInput v-if="correction.field === 'TITLE'" v-model="reviewedValue" :scope-name="`correction-${correction.id}`" field="proposedTitle" />
            <TextArea v-else v-model="reviewedValue" :scope-name="`correction-${correction.id}`" field="proposedContent" />
          </template>
          <template v-else>
            <h4>Proposed {{ correction.field === 'TITLE' ? 'title' : 'content' }}</h4>
            <p v-if="correction.field === 'TITLE'">{{ correction.proposedValue }}</p>
            <div v-else :class="RICH_CONTENT_PROSE" v-html="markdownizer.markdownToHtml(correction.proposedValue)" />
          </template>
        </template>
      </template>
    </NoteRealmLoader>
  </section>
</template>

<script setup lang="ts">
import { ref } from "vue"
import type {
  ConversationNoteCorrection,
  User,
} from "@generated/donut-backend-api"
import NoteRealmLoader from "@/components/notes/NoteRealmLoader.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import TextInput from "@/components/form/TextInput.vue"
import TextArea from "@/components/form/TextArea.vue"
import markdownizer from "@/components/form/markdownizer"
import { RICH_CONTENT_PROSE } from "@/constants/richContentProse"

const props = defineProps<{
  correction: ConversationNoteCorrection
  user: User
}>()
const reviewedValue = ref(props.correction.proposedValue ?? "")
</script>
