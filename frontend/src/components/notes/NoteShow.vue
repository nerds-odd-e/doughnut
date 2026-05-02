<template>
  <div class="note-show-container daisy-flex daisy-flex-col daisy-h-full">
    <NoteRealmLoader v-bind="{ noteId }" :key="reloadKey">
      <template #default="{ noteRealm }">
        <ContentLoader v-if="!noteRealm" />
        <template v-else>
          <template v-if="!isMinimized">
            <NoteToolbar
              v-if="currentUser"
              v-bind="{
                note: noteRealm.note,
                asMarkdown,
                conversationButton: noConversationButton,
                readonly: readonly(noteRealm),
              }"
              @note-accessory-updated="onNoteAccessoryUpdated"
              @edit-as-markdown="asMarkdown = $event"
            />
            <div
              class="note-content-wrapper daisy-flex-1 daisy-min-h-0 daisy-overflow-auto daisy-flex daisy-flex-col daisy-gap-4"
              :class="{ minimized: isMinimized }"
            >
              <div id="main-note-content" class="daisy-flex daisy-flex-col daisy-w-full">
                <NoteTextContent
                  v-bind="{
                    note: noteRealm.note,
                    asMarkdown,
                    readonly: readonly(noteRealm),
                    wikiTitles: noteRealm.wikiTitles ?? [],
                  }"
                  @dead-link-click="onDeadLinkClick"
                />
                <NoteAccessoryAsync
                  v-bind="{ noteId: noteRealm.id, updatedNoteAccessory, readonly: readonly(noteRealm) }"
                  :key="noteRealm.id"
                />
                <NoteRecentUpdateIndicator
                  v-bind="{
                    id: noteRealm.id,
                    updatedAt: noteRealm.note.updatedAt,
                  }"
                >
                  <p>
                    <span class="daisy-mr-3">
                      Created: {{ toLocalDateString(noteRealm.note.createdAt) }}
                    </span>
                    <span>
                      Last updated: {{ toLocalDateString(noteRealm.note.updatedAt) }}
                    </span>
                  </p>
                </NoteRecentUpdateIndicator>
                <template v-if="(noteRealm.references?.length ?? 0) > 0">
                  <h3 class="daisy-text-lg daisy-font-medium daisy-mb-2">References</h3>
                  <NoteReferences
                    v-bind="{ expandChildren, readonly: readonly(noteRealm) }"
                    :notes="noteRealm.references ?? []"
                  />
                </template>
              </div>
            </div>
          </template>

          <slot
            name="note-conversation"
            :note-realm="noteRealm"
          />

          <NoteDeadLinkCreateModal
            v-model="pendingDeadLinkTitle"
            :notebook-id="noteRealm.notebookId"
            :folder-id="noteRealm.note.noteTopology.folderId ?? undefined"
            :source-note-id="noteRealm.id"
          />
        </template>
      </template>
    </NoteRealmLoader>
  </div>
</template>

<script setup lang="ts">
import { inject, ref, watch, type Ref } from "vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import NoteRealmLoader from "./NoteRealmLoader.vue"
import type {
  NoteAccessory,
  NoteRealm,
  User,
} from "@generated/doughnut-backend-api"
import NoteTextContent from "./core/NoteTextContent.vue"
import NoteReferences from "./NoteReferences.vue"
import NoteAccessoryAsync from "./accessory/NoteAccessoryAsync.vue"
import NoteToolbar from "./core/NoteToolbar.vue"
import NoteRecentUpdateIndicator from "./NoteRecentUpdateIndicator.vue"
import NoteDeadLinkCreateModal from "./NoteDeadLinkCreateModal.vue"
const props = defineProps({
  noteId: { type: Number, required: true },
  expandChildren: { type: Boolean, required: true },
  noConversationButton: { type: Boolean, default: false },
  isMinimized: { type: Boolean, default: false },
})

const currentUser = inject<Ref<User | undefined>>("currentUser")
const readonly = (noteRealm: NoteRealm) =>
  !currentUser?.value || noteRealm?.fromBazaar === true

const pendingDeadLinkTitle = ref<string | null>(null)

const onDeadLinkClick = (title: string) => {
  pendingDeadLinkTitle.value = title
}

const updatedNoteAccessory = ref<NoteAccessory | undefined>(undefined)
const reloadKey = ref(0)
const onNoteAccessoryUpdated = () => {
  reloadKey.value += 1
}
const asMarkdown = ref(false)

watch(
  () => props.noteId,
  () => {
    asMarkdown.value = false
  }
)

const toLocalDateString = (date: string) => new Date(date).toLocaleDateString()
</script>
<style scoped>
.note-show-container {
  display: flex;
  flex-direction: column;
}

.note-content-wrapper {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  gap: 1rem;
}

.note-content-wrapper.minimized {
  height: 3rem;
  overflow: hidden;
}
</style>
