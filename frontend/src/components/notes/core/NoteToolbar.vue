<template>
  <nav class="daisy-navbar daisy-bg-base-200">
    <div class="daisy-btn-group daisy-btn-group-sm">
      <NoteNewButton
        button-title="Add Child Note"
        v-bind="{ referenceNote: note, insertMode: 'as-child', storageAccessor }"
      >
        <SvgAddChild />
      </NoteNewButton>

      <NoteNewButton
        v-if="note.parentId"
        button-title="Add Next Sibling Note"
        v-bind="{ referenceNote: note, insertMode: 'after', storageAccessor }"
      >
        <SvgAddSibling />
      </NoteNewButton>

      <PopButton title="search and link note">
        <template #button_face>
          <SvgSearchForLink />
        </template>
        <template #default="{ closer }">
          <LinkNoteDialog
            v-bind="{ note, storageAccessor }"
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

      <WikidataButton v-bind="{ note, storageAccessor }" />

      <button v-if="!asMarkdown" class="btn" title="Edit as markdown" @click="$emit('edit-as-markdown', true)">
        <SvgMarkdown />
      </button>
      <button v-else class="btn" title="Edit as rich content" @click="$emit('edit-as-markdown', false)">
        <SvgRichContent />
      </button>

      <button class="btn" title="Audio tools" v-if="!audioTools" @click="audioTools = true">
        <SvgAudioInput />
      </button>

      <div class="dropdown">
        <button
          id="dropdownMenuButton"
          aria-expanded="false"
          aria-haspopup="true"
          class="btn dropdown-toggle"
          data-bs-toggle="dropdown"
          role="button"
          title="more options"
        >
          <SvgCog />
        </button>
        <div class="dropdown-menu dropdown-menu-end">
          <PopButton
            btn-class="dropdown-item btn-primary"
            title="Note Recall Settings"
          >
            <NoteInfoBar v-bind="{ noteId: note.id }" />
          </PopButton>

          <PopButton title="Test me" sidebar="right">
            <template #button_face>
              <SvgRobot />
              <span class="ms-2">Test me</span>
            </template>
            <template #default="{ closer }">
              <NoteTestMeDialog
                v-bind="{ selectedNote: note, storageAccessor }"
                @close-dialog="closer"
              />
            </template>
          </PopButton>

          <PopButton
            btn-class="dropdown-item btn-primary"
            title="Generate Image with DALL-E"
          >
            <AIGenerateImageDialog v-bind="{ note, storageAccessor }" />
          </PopButton>

          <PopButton
            btn-class="dropdown-item btn-primary"
            title="Edit Note Image"
          >
            <template #button_face>
              <SvgImage />
              <span class="ms-2">Edit Note Image</span>
            </template>
            <template #default="{ closer }">
              <NoteEditImageDialog
                v-bind="{ noteId: note.id }"
                @close-dialog="noteAccessoriesUpdated(closer, $event)"
              />
            </template>
          </PopButton>

          <PopButton
            btn-class="dropdown-item btn-primary"
            title="Edit Note URL"
          >
            <template #button_face>
              <SvgUrlIndicator />
              <span class="ms-2">Edit Note URL</span>
            </template>
            <template #default="{ closer }">
              <NoteEditUrlDialog
                v-bind="{ noteId: note.id }"
                @close-dialog="noteAccessoriesUpdated(closer, $event)"
              />
            </template>
          </PopButton>

          <PopButton
            btn-class="dropdown-item btn-primary"
            title="Questions for the note"
          >
            <Questions v-bind="{ note }" />
          </PopButton>
          <NoteDeleteButton
            class="dropdown-item"
            v-bind="{ noteId: note.id, storageAccessor }"
          />
        </div>
      </div>
    </div>
  </nav>
  <NoteAudioTools
    v-if="audioTools"
    v-bind="{ note, storageAccessor }"
    @close-dialog="audioTools = false"
  />

</template>

<script setup lang="ts">
import { ref } from "vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { Note } from "@/generated/backend"
import type { NoteAccessory } from "@/generated/backend"
import NoteNewButton from "./NoteNewButton.vue"
import SvgAddChild from "../../svgs/SvgAddChild.vue"
import SvgAddSibling from "../../svgs/SvgAddSibling.vue"
import WikidataButton from "./WikidataButton.vue"
import SvgSearchForLink from "../../svgs/SvgSearchForLink.vue"
import LinkNoteDialog from "../../links/LinkNoteDialog.vue"
import SvgCog from "../../svgs/SvgCog.vue"
import NoteDeleteButton from "./NoteDeleteButton.vue"
import PopButton from "../../commons/Popups/PopButton.vue"
import AIGenerateImageDialog from "../AIGenerateImageDialog.vue"
import NoteTestMeDialog from "../NoteTestMeDialog.vue"
import Questions from "../Questions.vue"
import NoteInfoBar from "../NoteInfoBar.vue"
import SvgMarkdown from "@/components/svgs/SvgMarkdown.vue"
import SvgRichContent from "@/components/svgs/SvgRichContent.vue"
import SvgImage from "../../svgs/SvgImage.vue"
import SvgAudioInput from "../../svgs/SvgAudioInput.vue"
import SvgUrlIndicator from "../../svgs/SvgUrlIndicator.vue"
import NoteEditImageDialog from "../accessory/NoteEditImageDialog.vue"
import NoteEditUrlDialog from "../accessory/NoteEditUrlDialog.vue"
import NoteAudioTools from "../accessory/NoteAudioTools.vue"
import SvgRobot from "@/components/svgs/SvgRobot.vue"
import { useRouter } from "vue-router"
import SvgChat from "@/components/svgs/SvgChat.vue"

const { storageAccessor, note } = defineProps<{
  storageAccessor: StorageAccessor
  note: Note
  asMarkdown?: boolean
  conversationButton?: boolean
}>()

const audioTools = ref(false)

const router = useRouter()

const emit = defineEmits(["note-accessory-updated", "edit-as-markdown"])

const noteAccessoriesUpdated = (closer: () => void, na: NoteAccessory) => {
  if (na) {
    emit("note-accessory-updated", na)
  }
  closer()
}
</script>
