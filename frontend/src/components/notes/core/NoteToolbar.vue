<template>
  <nav class="daisy-navbar daisy-bg-base-200">
    <div class="daisy-btn-group daisy-btn-group-sm">
      <button
        v-if="!readonly && isHeadNote"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        title="Edit notebook settings"
        @click="editNotebookSettings"
      >
        <SvgNotebook />
      </button>

      <NoteNewButton
        v-if="!readonly"
        button-title="Add Child Note"
        v-bind="{ referenceNote: note, insertMode: 'as-child' }"
      >
        <SvgAddChild />
      </NoteNewButton>

      <NoteNewButton
        v-if="!readonly && note.parentId"
        button-title="Add Next Sibling Note"
        v-bind="{ referenceNote: note, insertMode: 'after' }"
      >
        <SvgAddSibling />
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
        <SvgChat />
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
        <SvgMarkdown />
      </button>
      <button v-else-if="!readonly" class="daisy-btn daisy-btn-ghost daisy-btn-sm" title="Edit as rich content" @click="$emit('edit-as-markdown', false)">
        <SvgRichContent />
      </button>

      <button v-if="!readonly && !audioTools" class="daisy-btn daisy-btn-ghost daisy-btn-sm" title="Audio tools" @click="audioTools = true">
        <SvgAudioInput />
      </button>

      <button
        v-if="!readonly"
        :class="['daisy-btn daisy-btn-ghost daisy-btn-sm', { 'daisy-btn-active': moreOptions }]"
        title="more options"
        @click="moreOptions = !moreOptions"
      >
        <SvgCog />
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
import type { Note, Notebook } from "@generated/backend"
import type { NoteAccessory } from "@generated/backend"
import NoteNewButton from "./NoteNewButton.vue"
import SvgAddChild from "../../svgs/SvgAddChild.vue"
import SvgAddSibling from "../../svgs/SvgAddSibling.vue"
import SvgSearchForLink from "../../svgs/SvgSearchForLink.vue"
import AddRelationshipDialog from "../../links/AddRelationshipDialog.vue"
import SvgCog from "../../svgs/SvgCog.vue"
import SvgMarkdown from "@/components/svgs/SvgMarkdown.vue"
import SvgRichContent from "@/components/svgs/SvgRichContent.vue"
import SvgAudioInput from "../../svgs/SvgAudioInput.vue"
import NoteAudioTools from "../accessory/NoteAudioTools.vue"
import { useRouter } from "vue-router"
import SvgChat from "@/components/svgs/SvgChat.vue"
import SvgWikidata from "../../svgs/SvgWikidata.vue"
import WikidataAssociationForNoteDialog from "../WikidataAssociationForNoteDialog.vue"
import NoteMoreOptionsDialog from "../accessory/NoteMoreOptionsDialog.vue"
import SvgNotebook from "../../svgs/SvgNotebook.vue"

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

const isHeadNote = computed(() => {
  return notebook !== undefined && notebook.headNoteId === note.id
})

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
