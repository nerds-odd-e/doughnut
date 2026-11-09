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
                >
                  <template #after-title>
                    <AssimilationPanel
                      v-if="assimilating(noteRealm) && !readFirst"
                      :note="noteRealm.note"
                    />
                  </template>
                </NoteTextContent>
                <ShowImage :note="noteRealm.note" />
                <NoteTimestamps :note-topology="noteRealm.note.noteTopology" />
                <NoteReferences
                  class="note-reading-column"
                  :note-topologies="noteRealm.references ?? []"
                />
              </div>
            </div>
            <!-- Sticky, because the notebook column can scroll this page past its bottom edge. -->
            <div
              v-if="assimilating(noteRealm) && readFirst"
              class="sticky bottom-0 z-10"
            >
              <AssimilationPanel
                read-first
                :note="noteRealm.note"
              />
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
import NoteTimestamps from "./NoteTimestamps.vue"
import NoteToolbar from "./core/NoteToolbar.vue"
import NoteUnresolvedWikiLinkModal from "./NoteUnresolvedWikiLinkModal.vue"
import AssimilationPanel from "@/components/recall/AssimilationPanel.vue"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useNarrowerThan } from "@/composables/useNarrowerThan"
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

const { isOpenForNote } = useAssimilationView()

const root = ref<HTMLElement | null>(null)
/**
 * A note column narrower than the full reading measure (48rem), as on a phone,
 * an iPad in portrait or a window narrowed by the notebook sidebar, puts the
 * note first and docks its assimilation in a bottom bar.
 */
const READ_FIRST_BELOW_WIDTH_PX = 768
const readFirst = useNarrowerThan(root, READ_FIRST_BELOW_WIDTH_PX)
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
const assimilating = (noteRealm: NoteRealm) =>
  !readonly(noteRealm) && isOpenForNote(noteRealm.note.id)

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
