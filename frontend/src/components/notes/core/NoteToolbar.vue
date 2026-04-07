<template>
  <nav class="daisy-navbar daisy-bg-base-200">
    <div class="daisy-btn-group daisy-btn-group-sm">
      <button
        v-if="!readonly && isHeadNote"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        title="Edit notebook settings"
        @click="editNotebookSettings"
      >
        <BookText class="w-5 h-5" />
      </button>

      <NoteNewButton
        v-if="!readonly"
        button-title="Add Child Note"
        v-bind="{ referenceNote: note, insertMode: 'as-child' }"
      >
        <FolderPlus class="w-5 h-5" />
      </NoteNewButton>

      <NoteNewButton
        v-if="!readonly && note.parentId"
        button-title="Add Next Sibling Note"
        v-bind="{ referenceNote: note, insertMode: 'after' }"
      >
        <Folders class="w-5 h-5" />
      </NoteNewButton>

      <PopButton v-if="!readonly" title="search and add relationship">
        <template #button_face>
          <SvgSearchForLink />
        </template>
        <template #default="{ closer }">
          <AddRelationshipDialog
            v-bind="{ note }"
            @close-dialog="closer"
          />
        </template>
      </PopButton>

      <a
        v-if="!conversationButton"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        role="button"
        @click="() => router.push({
          name: 'noteShow',
          params: { noteId: note.id },
          query: { conversation: 'true' }
        })"
        title="Star a conversation about this note"
      >
        <MessageCircle class="w-5 h-5" />
      </a>

      <PopButton v-if="!readonly" title="associate wikidata">
        <template #button_face>
          <SvgWikidata :class="{ 'wikidata-has-value': note.wikidataId }" />
        </template>
        <template #default="{ closer }">
          <WikidataAssociationForNoteDialog
            :note="note"
            @close-dialog="closer"
          />
        </template>
      </PopButton>

      <button v-if="!readonly && !asMarkdown" class="daisy-btn daisy-btn-ghost daisy-btn-sm" title="Edit as markdown" @click="$emit('edit-as-markdown', true)">
        <FileCode class="w-5 h-5" />
      </button>
      <button v-else-if="!readonly" class="daisy-btn daisy-btn-ghost daisy-btn-sm" title="Edit as rich content" @click="$emit('edit-as-markdown', false)">
        <LayoutTemplate class="w-5 h-5" />
      </button>

      <button v-if="!readonly && !audioTools" class="daisy-btn daisy-btn-ghost daisy-btn-sm" title="Audio tools" @click="audioTools = true">
        <Mic class="w-5 h-5" />
      </button>

      <button
        v-if="!readonly"
        :class="['daisy-btn daisy-btn-ghost daisy-btn-sm', { 'daisy-btn-active': moreOptions }]"
        title="more options"
        @click="moreOptions = !moreOptions"
      >
        <Settings class="w-5 h-5" />
      </button>
    </div>
  </nav>
  <NoteAudioTools
    v-if="!readonly && audioTools"
    v-bind="{ note }"
    @close-dialog="audioTools = false"
  />
  <NoteMoreOptionsDialog
    v-if="!readonly && moreOptions"
    v-bind="{ note }"
    @close-dialog="moreOptions = false"
    @note-accessory-updated="noteAccessoriesUpdated($event)"
  />

</template>

<script setup lang="ts">
import { ref, computed, watch } from "vue"
import type { Note, Notebook } from "@generated/doughnut-backend-api"
import type { NoteAccessory } from "@generated/doughnut-backend-api"
import NoteNewButton from "./NoteNewButton.vue"
import SvgSearchForLink from "../../svgs/SvgSearchForLink.vue"
import AddRelationshipDialog from "../../links/AddRelationshipDialog.vue"
import {
  BookText,
  FileCode,
  FolderPlus,
  Folders,
  LayoutTemplate,
  MessageCircle,
  Mic,
  Settings,
} from "lucide-vue-next"
import NoteAudioTools from "../accessory/NoteAudioTools.vue"
import { useRouter } from "vue-router"
import SvgWikidata from "../../svgs/SvgWikidata.vue"
import WikidataAssociationForNoteDialog from "../WikidataAssociationForNoteDialog.vue"
import NoteMoreOptionsDialog from "../accessory/NoteMoreOptionsDialog.vue"

const { note, notebook } = defineProps<{
  note: Note
  notebook?: Notebook
  asMarkdown?: boolean
  conversationButton?: boolean
  readonly?: boolean
}>()

const audioTools = ref(false)
const moreOptions = ref(false)

const router = useRouter()

const emit = defineEmits(["note-accessory-updated", "edit-as-markdown"])

const isHeadNote = computed(
  () => notebook !== undefined && notebook.headNoteId === note.id
)

watch(
  () => note.id,
  () => {
    moreOptions.value = false
  }
)

const editNotebookSettings = () => {
  if (notebook) {
    router.push({
      name: "notebookEdit",
      params: { notebookId: notebook.id },
    })
  }
}

const noteAccessoriesUpdated = (na: NoteAccessory) => {
  if (na) {
    emit("note-accessory-updated", na)
  }
}
</script>

<style scoped>
.wikidata-has-value {
  filter: drop-shadow(0 0 4px hsl(var(--p) / 0.6));
  color: hsl(var(--p));
}
</style>
