<template>
  <nav
    ref="toolbarNavRef"
    data-note-toolbar
    :class="[noteChromeToolbarNavClass, 'relative z-20']"
  >
    <div class="daisy-btn-group daisy-btn-group-sm">
      <NoteCreationNewButton
        v-if="showRelocatedNewNote"
        v-show="!newOverflowed"
        ref="newNoteButtonRef"
        :notebook-id="notebookId"
        :active-note-realm="activeNoteRealm"
        :breadcrumb-folders="breadcrumbFolders"
      />
      <PopButton
        v-if="!readonly"
        ref="wikiLinkOrRelationshipPopButtonRef"
        :hidden="wikiOverflowed || undefined"
        :aria-label="wikiLinkOrRelationshipLabel"
        :title="noteMoreOptionsTitles.wiki"
        :show-close-button="false"
      >
        <template #button_face>
          <SvgSearchForWikiLinkOrRelationship />
        </template>
        <template #default="{ closer }">
          <SearchForm
            v-bind="{ note }"
            :modal-closer="closer"
            @close-dialog="closer"
          />
        </template>
      </PopButton>

      <a
        v-if="!conversationButton && !conversationOverflowed"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        role="button"
        :aria-label="noteMoreOptionsTitles.conversation"
        @click="() => router.push({
          ...noteShowLocation(note.noteTopology.id),
          query: { conversation: 'true' },
        })"
        :title="noteMoreOptionsTitles.conversation"
      >
        <MessageCircle class="w-6 h-6" />
      </a>

      <button
        v-if="!readonly && !editOverflowed"
        type="button"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        :title="editTitle"
        :aria-label="editTitle"
        @click="emit('edit-as-markdown', !asMarkdown)"
      >
        <LayoutTemplate v-if="asMarkdown" class="w-6 h-6" />
        <FileCode v-else class="w-6 h-6" />
      </button>

      <NoteToolbarMoreOptions
        v-if="!readonly"
        ref="moreOptionsRef"
        :note="note"
        :toolbar-nav="toolbarNavRef"
        :as-markdown="asMarkdown"
        :has-new-note="showRelocatedNewNote"
        :has-conversation="!conversationButton"
        @overflowed-ids="overflowedIds = $event"
        @edit-as-markdown="emit('edit-as-markdown', $event)"
        @open-wiki="wikiLinkOrRelationshipPopButtonRef?.openDialog()"
        @open-new="newNoteButtonRef?.openDialog()"
      />
    </div>
  </nav>
  <NoteToolbarPanelShell v-if="!readonly && isPanelOpen">
    <NoteAudioTools v-if="isAudioOpen" v-bind="{ note }" />
    <AssimilationPanel
      v-else-if="isOpenForNote(note.id)"
      :key="assimilationPanelKey"
      :note="note"
      @reload-needed="onAssimilationReloadNeeded"
    />
  </NoteToolbarPanelShell>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue"
import type { Folder, Note, NoteRealm } from "@generated/doughnut-backend-api"
import SvgSearchForWikiLinkOrRelationship from "../../svgs/SvgSearchForWikiLinkOrRelationship.vue"
import SearchForm from "../../wiki-link-or-relationship/SearchForm.vue"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import { FileCode, LayoutTemplate, MessageCircle } from "@lucide/vue"
import NoteAudioTools from "../widgets/NoteAudioTools.vue"
import AssimilationPanel from "@/components/recall/AssimilationPanel.vue"
import NoteToolbarPanelShell from "./NoteToolbarPanelShell.vue"
import { useNoteToolbarPanel } from "@/composables/useNoteToolbarPanel"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { useRouter } from "vue-router"
import NoteToolbarMoreOptions from "../widgets/NoteToolbarMoreOptions.vue"
import { noteChromeToolbarNavClass } from "../noteChromeToolbarNavClass"
import { noteShowLocation } from "@/routes/noteShowLocation"
import NoteCreationNewButton from "../NoteCreationNewButton.vue"
import { useNotebookSidebarOpened } from "@/composables/notebookSidebarOpened"
import { useKeyboardShortcut } from "@/composables/useKeyboardShortcut"
import { useNoteShortcutScope } from "@/composables/noteShortcutScope"
import {
  noteMoreOptionsTitles,
  noteToolbarEditTitle,
  type NoteMoreOptionsActionId,
} from "../widgets/noteMoreOptionsTitles"

const wikiLinkOrRelationshipLabel = "Wiki link or relationship"

const props = withDefaults(
  defineProps<{
    note: Note
    notebookId: number
    activeNoteRealm?: NoteRealm
    breadcrumbFolders?: Folder[]
    asMarkdown?: boolean
    conversationButton?: boolean
    readonly?: boolean
  }>(),
  { breadcrumbFolders: () => [] }
)

const sidebarOpened = useNotebookSidebarOpened()

const showRelocatedNewNote = computed(
  () => !sidebarOpened.value && props.readonly !== true
)

const { isAudioOpen, isPanelOpen } = useNoteToolbarPanel()
const { isOpenForNote } = useAssimilationView()
const storageAccessor = useStorageAccessor()
const assimilationPanelKey = ref(0)

const onAssimilationReloadNeeded = async () => {
  await storageAccessor.value.storedApi().loadNoteRealm(props.note.id)
  assimilationPanelKey.value += 1
}
const toolbarNavRef = ref<HTMLElement | null>(null)
const wikiLinkOrRelationshipPopButtonRef = ref<InstanceType<
  typeof PopButton
> | null>(null)
const newNoteButtonRef = ref<InstanceType<typeof NoteCreationNewButton> | null>(
  null
)
const moreOptionsRef = ref<InstanceType<typeof NoteToolbarMoreOptions> | null>(
  null
)
const overflowedIds = ref<NoteMoreOptionsActionId[]>([])
const editOverflowed = computed(() => overflowedIds.value.includes("edit"))
const conversationOverflowed = computed(() =>
  overflowedIds.value.includes("conversation")
)
const wikiOverflowed = computed(() => overflowedIds.value.includes("wiki"))
const newOverflowed = computed(() => overflowedIds.value.includes("new"))
const editTitle = computed(() => noteToolbarEditTitle(props.asMarkdown))

const router = useRouter()
const shortcutScope = useNoteShortcutScope()

const emit = defineEmits<{
  (e: "edit-as-markdown", value: boolean): void
}>()

useKeyboardShortcut(
  "note-toggle-edit-mode",
  () => emit("edit-as-markdown", !props.asMarkdown),
  () => !props.readonly && shortcutScope.value
)

useKeyboardShortcut(
  "wiki-link-or-relationship",
  () => wikiLinkOrRelationshipPopButtonRef.value?.openDialog(),
  () => !props.readonly && shortcutScope.value
)

watch(
  () => props.note.id,
  () => {
    moreOptionsRef.value?.closeOverflowMenu()
  }
)
</script>
