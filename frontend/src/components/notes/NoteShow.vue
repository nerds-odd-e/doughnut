<template>
  <div class="note-show-container">
    <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
      <template #default="{ noteRealm }">
        <TeleportToHeadStatus>
          <div class="daisy-btn-group">
            <button
              v-if="onToggleSidebar"
              role="button"
              class="daisy-btn daisy-btn-sm daisy-btn-ghost"
              title="toggle sidebar"
              @click="(e: MouseEvent) => onToggleSidebar?.(e)"
            >
            <div class="daisy-w-4 daisy-h-4 daisy-flex daisy-items-center daisy-justify-center">
              <span class="daisy-btn-square-icon"></span>
            </div>
            </button>
          </div>
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
              class="note-content-wrapper"
              :class="{ 'daisy-collapse daisy-collapse-arrow': isMinimized }"
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
              <div class="daisy-w-full lg:daisy-w-3/12 inboundReferences" v-if="noteRealm.inboundReferences && noteRealm.inboundReferences.length > 0">
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
import type { NoteAccessory, User } from "@/generated/backend"
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
  @apply daisy-flex daisy-flex-col daisy-h-full;
}

.note-content-wrapper {
  @apply daisy-flex-1 daisy-min-h-0 daisy-overflow-auto daisy-flex daisy-flex-col lg:daisy-flex-row daisy-gap-4;
}

#main-note-content {
  @apply daisy-flex daisy-flex-col daisy-w-full lg:daisy-w-9/12;
}

.inboundReferences {
  @apply daisy-border-l daisy-border-base-300 daisy-pl-4 daisy-w-full lg:daisy-w-3/12 daisy-bg-amber-50/50;
}

.note-content-wrapper.minimized {
  @apply daisy-h-12 daisy-overflow-hidden;
}

.daisy-btn-group {
  @apply daisy-flex daisy-items-center;
}

.daisy-btn {
  @apply daisy-min-w-[2rem] daisy-min-h-[2rem] daisy-flex daisy-items-center daisy-justify-center;
}

.daisy-btn-square-icon {
  @apply daisy-flex daisy-items-center daisy-justify-center daisy-text-base daisy-leading-none;
}
</style>
