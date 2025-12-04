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
                notebook: noteRealm.notebook,
                asMarkdown,
                conversationButton: noConversationButton,
                readonly: readonly(noteRealm),
              }"
              @note-accessory-updated="onNoteAccessoryUpdated"
              @edit-as-markdown="asMarkdown = $event"
            />
            <div
              class="note-content-wrapper daisy-flex-1 daisy-min-h-0 daisy-overflow-auto daisy-flex daisy-flex-col lg:daisy-flex-row daisy-gap-4"
              :class="{ minimized: isMinimized }"
            >
              <div id="main-note-content" class="daisy-flex daisy-flex-col daisy-w-full lg:daisy-w-9/12">
                <NoteTextContent
                  v-bind="{
                    note: noteRealm.note,
                    asMarkdown,
                    readonly: readonly(noteRealm),
                  }"
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
                <ChildrenNotes
                  v-bind="{ expandChildren, readonly: readonly(noteRealm) }"
                  :notes="noteRealm.children ?? []"
                />
              </div>
              <div
	        v-if="noteRealm.inboundReferences && noteRealm.inboundReferences?.length > 0"
                class="daisy-w-full lg:daisy-w-3/12 daisy-border-l daisy-border-base-300 daisy-pl-4 daisy-bg-amber-50/50"
	      >
                <h3 class="daisy-text-lg daisy-font-medium daisy-mb-2">Referenced by</h3>
                <ul class="daisy-menu daisy-rounded-lg daisy-shadow-sm">
                  <li v-for="link in noteRealm.inboundReferences"
                      :key="link.id"
                      class="daisy-menu-item daisy-hover:daisy-bg-base-200 daisy-transition-colors daisy-py-2"
                  >
                    <div class="daisy-flex daisy-items-center daisy-gap-2">
                      <span class="daisy-text-sm daisy-text-base-content/70">
                        {{ reverseLabel(link.noteTopology.linkType) }}
                      </span>
                      <LinkOfNote
                        class="link-multi"
                        :key="link.id"
                        v-bind="{ note: link }"
                        :reverse="true"
                      />
                    </div>
                  </li>
                </ul>
              </div>
            </div>
          </template>

          <slot
            name="note-conversation"
            :note-realm="noteRealm"
          />
        </template>
      </template>
    </NoteRealmLoader>
  </div>
</template>

<script setup lang="ts">
import { inject, ref, type Ref } from "vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import NoteRealmLoader from "./NoteRealmLoader.vue"
import type { NoteAccessory, NoteRealm, User } from "@generated/backend"
import NoteTextContent from "./core/NoteTextContent.vue"
import ChildrenNotes from "./ChildrenNotes.vue"
import NoteAccessoryAsync from "./accessory/NoteAccessoryAsync.vue"
import NoteToolbar from "./core/NoteToolbar.vue"
import NoteRecentUpdateIndicator from "./NoteRecentUpdateIndicator.vue"
import LinkOfNote from "../links/LinkOfNote.vue"
import { reverseLabel } from "../../models/linkTypeOptions"

defineProps({
  noteId: { type: Number, required: true },
  expandChildren: { type: Boolean, required: true },
  noConversationButton: { type: Boolean, default: false },
  isMinimized: { type: Boolean, default: false },
})

const currentUser = inject<Ref<User | undefined>>("currentUser")
const readonly = (noteRealm: NoteRealm) => {
  return !currentUser?.value || noteRealm?.fromBazaar === true
}

const updatedNoteAccessory = ref<NoteAccessory | undefined>(undefined)
const reloadKey = ref(0)
const onNoteAccessoryUpdated = () => {
  reloadKey.value += 1
}
const asMarkdown = ref(false)

const toLocalDateString = (date: string) => {
  return new Date(date).toLocaleDateString()
}
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
