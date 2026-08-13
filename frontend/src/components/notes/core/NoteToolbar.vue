<template>
  <nav
    ref="toolbarNavRef"
    data-note-toolbar
    :class="[noteChromeToolbarNavClass, 'relative z-20']"
  >
    <div class="daisy-btn-group daisy-btn-group-sm">
      <NoteCreationNewButton
        v-if="showRelocatedNewNote"
        :notebook-id="notebookId"
        :active-note-realm="activeNoteRealm"
        :breadcrumb-folders="breadcrumbFolders"
      />
      <PopButton
        v-if="!readonly"
        ref="wikiLinkOrRelationshipPopButtonRef"
        :aria-label="wikiLinkOrRelationshipLabel"
        :title="`${wikiLinkOrRelationshipLabel} (Ctrl+Shift+F / Cmd+Shift+F)`"
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
        v-if="!conversationButton"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        role="button"
        aria-label="Star a conversation about this note"
        @click="() => router.push({
          ...noteShowLocation(note.noteTopology.id),
          query: { conversation: 'true' },
        })"
        title="Star a conversation about this note"
      >
        <MessageCircle class="w-6 h-6" />
      </a>

      <button v-if="!readonly && !asMarkdown" type="button" class="daisy-btn daisy-btn-ghost daisy-btn-sm" title="Edit as markdown (m)" aria-label="Edit as markdown (m)" @click="$emit('edit-as-markdown', true)">
        <FileCode class="w-6 h-6" />
      </button>
      <button v-else-if="!readonly" type="button" class="daisy-btn daisy-btn-ghost daisy-btn-sm" title="Edit as rich content (m)" aria-label="Edit as rich content (m)" @click="$emit('edit-as-markdown', false)">
        <LayoutTemplate class="w-6 h-6" />
      </button>

      <NoteToolbarMoreOptions
        v-if="!readonly"
        ref="moreOptionsRef"
        :note="note"
        :toolbar-nav="toolbarNavRef"
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
const moreOptionsRef = ref<InstanceType<typeof NoteToolbarMoreOptions> | null>(
  null
)

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
