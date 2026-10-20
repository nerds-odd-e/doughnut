<template>
  <div ref="root" class="note-show-container flex flex-col h-full">
    <NoteRealmLoader v-bind="{ noteId }">
      <template #default="{ noteRealm }">
        <ContentLoader v-if="!noteRealm" />
        <template v-else>
          <template v-if="!isMinimized">
            <NoteToolbar
              v-if="currentUser"
              ref="toolbar"
              v-bind="{
                note: noteRealm.note,
                notebookId: noteRealm.notebookRealm.notebook.id,
                activeNoteRealm: noteRealm,
                breadcrumbFolders: noteRealm.ancestorFolders ?? [],
                asMarkdown,
                readonly: readonly(noteRealm),
              }"
              @edit-as-markdown="asMarkdown = $event"
            />
            <div
              class="note-content-wrapper flex-1 min-h-0 overflow-auto flex flex-col gap-4"
            >
              <div id="main-note-content" class="flex flex-col w-full">
                <NoteTextContent
                  ref="noteTextContent"
                  v-bind="{
                    note: noteRealm.note,
                    asMarkdown,
                    readonly: readonly(noteRealm),
                    wikiLinks: noteRealm.wikiLinks ?? [],
                    isReadmeContext: isReadmeTitle(noteRealm),
                    hasInboundReferences: noteHasInboundWikiReferences(noteRealm),
                    trashed: isNoteRealmInTrash(noteRealm),
                  }"
                  @dead-wiki-link-click="onDeadWikiLinkClick"
                />
                <ShowImage :note="noteRealm.note" />
                <NoteReferences
                  :note-topologies="noteRealm.references ?? []"
                />
              </div>
            </div>
          </template>

          <slot
            name="note-conversation"
            :note-realm="noteRealm"
          />

          <NoteUnresolvedWikiLinkModal
            v-model="pendingDeadWikiLink"
            :notebook-id="noteRealm.notebookRealm.notebook.id"
            :note-realm="noteRealm"
          />
        </template>
      </template>
    </NoteRealmLoader>
  </div>
</template>

<script setup lang="ts">
import {
  inject,
  onMounted,
  onUnmounted,
  provide,
  ref,
  toRef,
  watch,
  type Ref,
} from "vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import NoteRealmLoader from "./NoteRealmLoader.vue"
import type { NoteRealm, User } from "@generated/donut-backend-api"
import NoteTextContent from "./core/NoteTextContent.vue"
import NoteReferences from "./NoteReferences.vue"
import ShowImage from "./widgets/ShowImage.vue"
import NoteToolbar from "./core/NoteToolbar.vue"
import NoteUnresolvedWikiLinkModal from "./NoteUnresolvedWikiLinkModal.vue"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"
import { isNoteRealmInTrash } from "@/utils/folderTrash"
import { provideMemoryTrackerActions } from "@/composables/useMemoryTrackerActions"
import {
  noteTitleDictationKey,
  useTitleDictation,
} from "@/composables/useTitleDictation"
import { isReservedReadmeNoteTitle } from "@/utils/reservedReadmeTitles"

const props = defineProps({
  noteId: { type: Number, required: true },
  isMinimized: { type: Boolean, default: false },
})

provideMemoryTrackerActions(toRef(() => props.noteId))

const root = ref<HTMLElement | null>(null)
const noteTextContent = ref<InstanceType<typeof NoteTextContent> | null>(null)
const titleDictation = useTitleDictation(() =>
  noteTextContent.value!.beginTitleDictation()
)
const toolbar = ref<InstanceType<typeof NoteToolbar> | null>(null)
const hasTitleIntent = ref(false)
const updateTitleIntent = (target: EventTarget | null) => {
  if (
    target instanceof Node &&
    target === root.value?.querySelector('[data-test="note-title"]')
  ) {
    hasTitleIntent.value = true
  } else if (
    !(target instanceof Node && toolbar.value?.containsFocusTarget(target))
  ) {
    hasTitleIntent.value = false
  }
}
const onFocusIn = (event: FocusEvent) => updateTitleIntent(event.target)
const onFocusOut = (event: FocusEvent) => updateTitleIntent(event.relatedTarget)
onMounted(() => {
  document.addEventListener("focusin", onFocusIn)
  document.addEventListener("focusout", onFocusOut)
})
onUnmounted(() => {
  document.removeEventListener("focusin", onFocusIn)
  document.removeEventListener("focusout", onFocusOut)
})
provide(noteTitleDictationKey, { dictation: titleDictation, hasTitleIntent })
watch(
  () => props.noteId,
  () => {
    hasTitleIntent.value = false
    titleDictation.leave()
  }
)

const currentUser = inject<Ref<User | undefined>>("currentUser")
const readonly = (noteRealm: NoteRealm) =>
  !currentUser?.value || noteRealm.notebookRealm.readonly === true

const isReadmeTitle = (noteRealm: NoteRealm) =>
  isReservedReadmeNoteTitle(noteRealm.note.noteTopology.title)

const noteHasInboundWikiReferences = (noteRealm: NoteRealm) =>
  (noteRealm.references?.length ?? 0) > 0

const pendingDeadWikiLink = ref<DeadWikiLinkPayload | null>(null)

const onDeadWikiLinkClick = (payload: DeadWikiLinkPayload) => {
  pendingDeadWikiLink.value = payload
}

const asMarkdown = ref(false)

watch(
  () => props.noteId,
  () => {
    asMarkdown.value = false
    pendingDeadWikiLink.value = null
  }
)
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
</style>
