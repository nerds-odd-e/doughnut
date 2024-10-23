<template>
  <nav class="navbar justify-content-between bg-light">
    <div class="btn-group btn-group-sm">
      <NoteNewButton
        button-title="Add Child Note"
        v-bind="{ parentId: note.id, storageAccessor }"
      >
        <SvgAddChild />
      </NoteNewButton>

      <WikidataButton v-bind="{ note, storageAccessor }" />
      <NoteDetailsAutoCompletionButton v-bind="{ note, storageAccessor }" />

      <PopButton title="Chat with AI" sidebar="right">
        <template #button_face>
          <SvgChat />
        </template>
        <template #default="{ closer }">
          <NoteChatDialog
            v-bind="{ selectedNote: note, storageAccessor }"
            @close-dialog="closer"
          />
        </template>
      </PopButton>

      <NoteSendMessageButton
        v-bind="{ noteId: note.id }"
      />

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

      <button v-if="!asMarkdown" class="btn" title="Edit as markdown" @click="$emit('edit-as-markdown', true)">
        <SvgMarkdown />
      </button>
      <button v-else class="btn" title="Edit as rich content" @click="$emit('edit-as-markdown', false)">
        <SvgRichContent />
      </button>


      <button class="btn" title="Move up" @click="moveUp">
        <SvgUp />
      </button>

      <button class="btn" title="Move down" @click="moveDown">
        <SvgDown />
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
            title="Note Review Settings"
          >
            <NoteInfoBar v-bind="{ noteId: note.id }" />
          </PopButton>
          <PopButton
            btn-class="dropdown-item btn-primary"
            title="Generate Image with DALL-E"
          >
            <AIGenerateImageDialog v-bind="{ note, storageAccessor }" />
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
    <div class="btn-group btn-group-sm ms-auto">
      <PopButton title="edit note image">
        <template #button_face>
          <SvgImage />
        </template>
        <template #default="{ closer }">
          <NoteEditImageDialog
            v-bind="{ noteId: note.id }"
            @close-dialog="noteAccessoriesUpdated(closer, $event)"
          />
        </template>
      </PopButton>

      <PopButton title="edit note url">
        <template #button_face>
          <SvgUrlIndicator />
        </template>
        <template #default="{ closer }">
          <NoteEditUrlDialog
            v-bind="{ noteId: note.id }"
            @close-dialog="noteAccessoriesUpdated(closer, $event)"
          />
        </template>
      </PopButton>

      <PopButton title="Upload audio">
        <template #button_face>
          <SvgResume />
        </template>
        <template #default="{ closer }">
          <NoteEditUploadAudioDialog
            v-bind="{ noteId: note.id }"
            @close-dialog="noteAccessoriesUpdated(closer, $event)"
          />
        </template>
      </PopButton>
    </div>
  </nav>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { Note } from "@/generated/backend"
import type { NoteAccessory } from "@/generated/backend"
import NoteNewButton from "./NoteNewButton.vue"
import SvgAddChild from "../../svgs/SvgAddChild.vue"
import WikidataButton from "./WikidataButton.vue"
import SvgSearchForLink from "../../svgs/SvgSearchForLink.vue"
import LinkNoteDialog from "../../links/LinkNoteDialog.vue"
import SvgCog from "../../svgs/SvgCog.vue"
import SvgChat from "../../svgs/SvgChat.vue"
import SvgUp from "../../svgs/SvgUp.vue"
import SvgDown from "../../svgs/SvgDown.vue"
import NoteDeleteButton from "./NoteDeleteButton.vue"
import PopButton from "../../commons/Popups/PopButton.vue"
import AIGenerateImageDialog from "../AIGenerateImageDialog.vue"
import NoteDetailsAutoCompletionButton from "./NoteDetailsAutoCompletionButton.vue"
import NoteChatDialog from "../NoteChatDialog.vue"
import Questions from "../Questions.vue"
import NoteInfoBar from "../NoteInfoBar.vue"
import SvgMarkdown from "@/components/svgs/SvgMarkdown.vue"
import SvgRichContent from "@/components/svgs/SvgRichContent.vue"
import NoteSendMessageButton from "./NoteSendMessageButton.vue"
import SvgImage from "../../svgs/SvgImage.vue"
import SvgResume from "../../svgs/SvgResume.vue"
import SvgUrlIndicator from "../../svgs/SvgUrlIndicator.vue"
import NoteEditImageDialog from "../accessory/NoteEditImageDialog.vue"
import NoteEditUploadAudioDialog from "../accessory/NoteEditUploadAudioDialog.vue"
import NoteEditUrlDialog from "../accessory/NoteEditUrlDialog.vue"

const props = defineProps({
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
  audioTools: {
    type: Boolean,
    required: true,
  },
  asMarkdown: Boolean,
})

const emit = defineEmits(["note-accessory-updated", "edit-as-markdown"])

const moveUp = () => {
  props.storageAccessor.storedApi().moveUp(props.note.id)
}
const moveDown = () => {
  props.storageAccessor.storedApi().moveDown(props.note.id)
}

const noteAccessoriesUpdated = (closer: () => void, na: NoteAccessory) => {
  if (na) {
    emit("note-accessory-updated", na)
  }
  closer()
}
</script>
