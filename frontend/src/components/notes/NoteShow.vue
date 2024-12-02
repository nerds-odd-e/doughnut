<template>
  <div class="note-show-container">
    <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
      <template #default="{ noteRealm }">
        <TeleportToHeadStatus>
          <div class="btn-group">
            <button
              v-if="onToggleSidebar"
              role="button"
              class="btn btn-sm"
              title="toggle sidebar"
              @click="(e: MouseEvent) => onToggleSidebar?.(e)"
            >
              <span class="navbar-toggler-icon"></span>
            </button>
          </div>
          <BreadcrumbWithCircle
            v-if="noteRealm"
            v-bind="{
              fromBazaar: noteRealm?.fromBazaar,
              circle: noteRealm.notebook?.circle,
              noteTopic: noteRealm?.note.noteTopic,
            }"
          />
        </TeleportToHeadStatus>

        <ContentLoader v-if="!noteRealm" />
        <template v-else>
          <template v-if="!conversationMaximized">
            <NoteToolbar
              v-if="!readonly"
              v-bind="{
                note: noteRealm.note,
                storageAccessor,
                asMarkdown,
                conversationButton: showConversationRef || noConversationButton,
              }"
              @note-accessory-updated="updatedNoteAccessory = $event"
              @edit-as-markdown="asMarkdown = $event"
            />
            <div
              :class="{
                'with-conversation': showConversation,
                minimized: conversationMaximized,
              }"
            >
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
          </template>

          <div
            class="conversation-wrapper"
            v-if="showConversationRef"
            :class="{ maximized: conversationMaximized }"
          >
            <NoteConversation
              :note-id="noteRealm.id"
              :storage-accessor="storageAccessor"
              :is-maximized="conversationMaximized"
              @close-dialog="handleCloseConversation"
              @toggle-maximize="conversationMaximized = !conversationMaximized"
            />
          </div>
        </template>
      </template>
    </NoteRealmLoader>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, type PropType, type Ref, watch } from "vue"
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
import NoteConversation from "../conversations/NoteConversation.vue"
import TeleportToHeadStatus from "@/pages/commons/TeleportToHeadStatus.vue"
import BreadcrumbWithCircle from "../../components/toolbars/BreadcrumbWithCircle.vue"
import { useRouter } from "vue-router"

const props = defineProps({
  noteId: { type: Number, required: true },
  expandChildren: { type: Boolean, required: true },
  noConversationButton: { type: Boolean, default: false },
  showConversation: { type: Boolean, default: false },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
  onToggleSidebar: { type: Function, required: false },
})

const router = useRouter()
const showConversationRef = ref(props.showConversation)

// Watch for prop changes
watch(
  () => props.showConversation,
  (newVal) => {
    showConversationRef.value = newVal
  }
)

// Update URL when conversation is closed
const handleCloseConversation = () => {
  showConversationRef.value = false
  router.replace({
    name: "noteShow",
    params: { noteId: props.noteId },
    query: {},
  })
}

const currentUser = inject<Ref<User | undefined>>("currentUser")
const readonly = computed(() => !currentUser?.value)

const updatedNoteAccessory = ref<NoteAccessory | undefined>(undefined)
const asMarkdown = ref(false)
const conversationMaximized = ref(false)

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

.with-conversation {
  flex: 1;
  min-height: 0;
  transition: height 0.3s ease;
  overflow: auto;
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

.minimized {
  height: 50px;
  overflow: hidden;
}

.conversation-wrapper.maximized {
  height: calc(100% - 50px);
}
</style>
