<template>
  <div class="note-show-container">
    <NoteCoreToolbar
      v-if="!readonly"
      v-bind="{ note: noteRealm.note, storageAccessor, asMarkdown, audioTools }"
      @note-accessory-updated="updatedNoteAccessory = $event"
      @edit-as-markdown="asMarkdown = $event"
      @show-audio-tools="audioTools = true"
      @show-conversations="showConversation = true"
    />

    <div class="note-content-wrapper" :class="{ 'with-conversation': showConversation }">
      <NoteAudioTools
        v-if="audioTools"
        v-bind="{ note: noteRealm.note, storageAccessor }"
        @close-dialog="
        audioTools = false;
        updatedNoteAccessory = $event
        "
      />
      <div id="main-note-content" class="col-md-9">
        <NoteTextContent
          v-bind="{
            note: noteRealm.note,
            asMarkdown,
            readonly,
            storageAccessor,
          }"
        />
        <NoteAccessoryAsync
          v-bind="{ noteId: noteRealm.id, updatedNoteAccessory, readonly }"
        />
        <NoteRecentUpdateIndicator
          v-bind="{
            id: noteRealm.id,
            updatedAt: noteRealm.note.updatedAt,
          }"
        >
          <p>
            <span class="me-3">
              Created: {{ toLocalDateString(noteRealm.note.createdAt) }}
            </span>
            <span>
              Last updated: {{ toLocalDateString(noteRealm.note.updatedAt) }}
            </span>
          </p>
        </NoteRecentUpdateIndicator>
        <ChildrenNotes
          v-bind="{ expandChildren, readonly, storageAccessor }"
          :notes="noteRealm.children ?? []"
        />
      </div>
      <div class="col-md-3 refers" v-if="noteRealm.refers">
        <ul>
          <li v-for="link in noteRealm.refers" :key="link.id">
            <span>{{ reverseLabel(link.noteTopic.linkType) }} </span>
            <LinkOfNote
              class="link-multi"
              :key="link.id"
              v-bind="{ note: link, storageAccessor }"
              :reverse="true"
            />
          </li>
        </ul>
      </div>
    </div>

    <div class="conversation-wrapper" v-if="showConversation">
      <NoteConversation
        v-if="showConversation"
        :note-id="noteRealm.id"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import type { NoteAccessory, NoteRealm } from "@/generated/backend"
import NoteTextContent from "./core/NoteTextContent.vue"
import ChildrenNotes from "./ChildrenNotes.vue"
import type { StorageAccessor } from "../../store/createNoteStorage"
import NoteAccessoryAsync from "./accessory/NoteAccessoryAsync.vue"
import NoteCoreToolbar from "./core/NoteCoreToolbar.vue"
import NoteRecentUpdateIndicator from "./NoteRecentUpdateIndicator.vue"
import LinkOfNote from "../links/LinkOfNote.vue"
import { reverseLabel } from "../../models/linkTypeOptions"
import NoteAudioTools from "./accessory/NoteAudioTools.vue"
import NoteConversation from "../conversations/NoteConversation.vue"

defineProps({
  noteRealm: { type: Object as PropType<NoteRealm>, required: true },
  expandChildren: { type: Boolean, required: true },
  expandInfo: { type: Boolean, default: false },
  readonly: { type: Boolean, default: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const updatedNoteAccessory = ref<NoteAccessory | undefined>(undefined)
const asMarkdown = ref(false)
const audioTools = ref(false)
const showConversation = ref(false)

const toLocalDateString = (date: string) => {
  return new Date(date).toLocaleDateString()
}
</script>

<style scoped>
.note-show-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.note-content-wrapper {
  flex: 1;
  min-height: 0;
  transition: height 0.3s ease;
  overflow: auto;
}

.note-content-wrapper.with-conversation {
  height: 50%;
}

.conversation-wrapper {
  height: 50%;
  border-top: 1px solid #e9ecef;
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f8f9fa;
  min-height: 0;
}

.refers {
  border-left: 1px solid #e9ecef;
}
</style>
