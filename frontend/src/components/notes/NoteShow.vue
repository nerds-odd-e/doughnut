<template>
  <div class="note-show-container daisy-flex daisy-flex-col daisy-h-full">
    <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
      <template #default="{ noteRealm }">
        <TeleportToHeadStatus>
          <button
            v-if="onToggleSidebar"
            role="button"
            class="daisy-btn daisy-btn-sm daisy-btn-ghost"
            :class="{ 'sidebar-expanded': isSidebarExpanded }"
            title="toggle sidebar"
            @click="(e: MouseEvent) => onToggleSidebar?.(e)"
          >
          <div class="daisy-w-4 daisy-h-4">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="w-4 h-4"
            >
              <template v-if="isSidebarExpanded">
                <path d="M19 12H5M12 19l-7-7 7-7"/>
              </template>
              <template v-else>
                <line x1="3" y1="6" x2="21" y2="6"></line>
                <line x1="6" y1="12" x2="21" y2="12"></line>
                <line x1="3" y1="18" x2="21" y2="18"></line>
              </template>
            </svg>
          </div>
          </button>
          <BreadcrumbWithCircle
            v-if="noteRealm"
            v-bind="{
              fromBazaar: noteRealm?.fromBazaar,
              circle: noteRealm.notebook?.circle,
              noteTopology: noteRealm?.note.noteTopology,
            }"
          />
        </TeleportToHeadStatus>

        <ContentLoader v-if="!noteRealm" />
        <template v-else>
          <template v-if="!isMinimized">
            <NoteToolbar
              v-if="!readonly"
              v-bind="{
                note: noteRealm.note,
                storageAccessor,
                asMarkdown,
                conversationButton: noConversationButton,
              }"
              @note-accessory-updated="updatedNoteAccessory = $event"
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
                    readonly,
                    storageAccessor,
                  }"
                />
                <NoteAccessoryAsync
                  v-bind="{ noteId: noteRealm.id, updatedNoteAccessory, readonly }"
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
                  v-bind="{ expandChildren, readonly, storageAccessor }"
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
                        v-bind="{ note: link, storageAccessor }"
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
import { computed, inject, ref, type PropType, type Ref } from "vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import NoteRealmLoader from "./NoteRealmLoader.vue"
import type { NoteAccessory, User } from "@generated/backend"
import NoteTextContent from "./core/NoteTextContent.vue"
import ChildrenNotes from "./ChildrenNotes.vue"
import type { StorageAccessor } from "../../store/createNoteStorage"
import NoteAccessoryAsync from "./accessory/NoteAccessoryAsync.vue"
import NoteToolbar from "./core/NoteToolbar.vue"
import NoteRecentUpdateIndicator from "./NoteRecentUpdateIndicator.vue"
import LinkOfNote from "../links/LinkOfNote.vue"
import { reverseLabel } from "../../models/linkTypeOptions"
import TeleportToHeadStatus from "@/pages/commons/TeleportToHeadStatus.vue"
import BreadcrumbWithCircle from "../../components/toolbars/BreadcrumbWithCircle.vue"

defineProps({
  noteId: { type: Number, required: true },
  expandChildren: { type: Boolean, required: true },
  noConversationButton: { type: Boolean, default: false },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
  onToggleSidebar: { type: Function, required: false },
  isMinimized: { type: Boolean, default: false },
  isSidebarExpanded: { type: Boolean, default: false },
})

const currentUser = inject<Ref<User | undefined>>("currentUser")
const readonly = computed(() => !currentUser?.value)

const updatedNoteAccessory = ref<NoteAccessory | undefined>(undefined)
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

