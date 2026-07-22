<template>
  <div class="note-show-container flex flex-col h-full">
    <NoteRealmLoader v-bind="{ noteId }">
      <template #default="{ noteRealm }">
        <ContentLoader v-if="!noteRealm" />
        <template v-else>
          <template v-if="!isMinimized">
            <div v-if="showBreadcrumb" class="breadcrumb-wrapper mb-2">
              <BreadcrumbWithCircle
                v-bind="{
                  ancestorFolders,
                  notebookRealm: noteRealm.notebookRealm,
                }"
              />
            </div>
            <NoteToolbar
              v-if="currentUser"
              v-bind="{
                note: noteRealm.note,
                notebookId: noteRealm.notebookRealm.notebook.id,
                activeNoteRealm: noteRealm,
                breadcrumbFolders:
                  ancestorFolders.length > 0
                    ? ancestorFolders
                    : (noteRealm.ancestorFolders ?? []),
                asMarkdown,
                conversationButton: noConversationButton,
                readonly: readonly(noteRealm),
              }"
              @edit-as-markdown="asMarkdown = $event"
            />
            <div
              class="note-content-wrapper flex-1 min-h-0 overflow-auto flex flex-col gap-4"
              :class="{ minimized: isMinimized }"
            >
              <div id="main-note-content" class="flex flex-col w-full">
                <NoteTextContent
                  v-bind="{
                    note: noteRealm.note,
                    asMarkdown,
                    readonly: readonly(noteRealm),
                    wikiTitles: noteRealm.wikiTitles ?? [],
                    isReadmeContext: isReadmeTitle(noteRealm),
                    hasInboundReferences: noteHasInboundWikiReferences(noteRealm),
                  }"
                  @dead-link-click="onDeadLinkClick"
                />
                <ShowImage
                  v-bind="{
                    ...noteImageScalarsFromMarkdown(noteRealm.note.content ?? ''),
                    opacity: 0.2,
                  }"
                  :key="noteRealm.id"
                />
                <NoteRecentUpdateIndicator
                  v-bind="{
                    id: noteRealm.id,
                    updatedAt: noteRealm.note.noteTopology.updatedAt,
                  }"
                >
                  <p>
                    <span class="mr-3">
                      Created: {{ toLocalDateString(noteRealm.note.noteTopology.createdAt) }}
                    </span>
                    <span>
                      Last updated: {{ toLocalDateString(noteRealm.note.noteTopology.updatedAt) }}
                    </span>
                  </p>
                </NoteRecentUpdateIndicator>
                <template v-if="noteHasInboundWikiReferences(noteRealm)">
                  <h3 class="text-lg font-medium mb-2">References</h3>
                  <NoteReferences
                    v-bind="{ expandChildren, readonly: readonly(noteRealm) }"
                    :note-topologies="noteRealm.references ?? []"
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
            v-model="pendingDeadLink"
            :notebook-id="noteRealm.notebookRealm.notebook.id"
            :note-realm="noteRealm"
            :source-note-id="noteRealm.id"
          />
        </template>
      </template>
    </NoteRealmLoader>
  </div>
</template>

<script setup lang="ts">
import { inject, ref, watch, type PropType, type Ref } from "vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import NoteRealmLoader from "./NoteRealmLoader.vue"
import type { Folder, NoteRealm, User } from "@generated/doughnut-backend-api"
import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue"
import NoteTextContent from "./core/NoteTextContent.vue"
import NoteReferences from "./NoteReferences.vue"
import ShowImage from "./widgets/ShowImage.vue"
import { noteImageScalarsFromMarkdown } from "@/utils/noteContentFrontmatter"
import NoteToolbar from "./core/NoteToolbar.vue"
import NoteRecentUpdateIndicator from "./NoteRecentUpdateIndicator.vue"
import NoteDeadLinkCreateModal from "./NoteDeadLinkCreateModal.vue"
import type { DeadLinkPayload } from "@/utils/wikiPropertyValueField"
import { provideNoteShortcutScope } from "@/composables/noteShortcutScope"
import { isReservedReadmeNoteTitle } from "@/utils/reservedReadmeTitles"

const props = defineProps({
  noteId: { type: Number, required: true },
  expandChildren: { type: Boolean, required: true },
  noConversationButton: { type: Boolean, default: false },
  isMinimized: { type: Boolean, default: false },
  showBreadcrumb: { type: Boolean, default: false },
  ownsShortcuts: { type: Boolean, default: false },
  ancestorFolders: {
    type: Array as PropType<Folder[]>,
    default: () => [],
  },
})

provideNoteShortcutScope(() => props.ownsShortcuts)

const currentUser = inject<Ref<User | undefined>>("currentUser")
const readonly = (noteRealm: NoteRealm) =>
  !currentUser?.value || noteRealm.notebookRealm.readonly === true

const isReadmeTitle = (noteRealm: NoteRealm) =>
  isReservedReadmeNoteTitle(noteRealm.note.noteTopology.title)

const noteHasInboundWikiReferences = (noteRealm: NoteRealm) =>
  (noteRealm.references?.length ?? 0) > 0

const pendingDeadLink = ref<DeadLinkPayload | null>(null)

const onDeadLinkClick = (payload: DeadLinkPayload) => {
  pendingDeadLink.value = payload
}

const asMarkdown = ref(false)

watch(
  () => props.noteId,
  () => {
    asMarkdown.value = false
    pendingDeadLink.value = null
  }
)

const toLocalDateString = (date: string | undefined) =>
  date ? new Date(date).toLocaleDateString() : ""
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
