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

      <PopButton title="Chat with AI">
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
          <PopButton btn-class="dropdown-item btn-primary" title="Add Question">
            <NoteAddQuestion v-bind="{ noteId: note.id }" />
          </PopButton>
          <PopButton
            btn-class="dropdown-item btn-primary"
            title="View Questions"
          >
            <NoteInfoBar v-bind="{ noteId: note.id }" />
          </PopButton>
          <PopButton
            btn-class="dropdown-item btn-primary"
            title="Start Assessment"
          >
            <AssessmentDialog />
          </PopButton>
          <NoteDeleteButton
            class="dropdown-item"
            v-bind="{ noteId: note.id, storageAccessor }"
          />
        </div>
      </div>
    </div>
    <NoteAccessoryToolbar
      v-bind="{ noteId: note.id }"
      @note-accessory-updated="$emit('note-accessory-updated', $event)"
    />
  </nav>
</template>

<script setup lang="ts">
import { PropType } from "vue";
import { StorageAccessor } from "@/store/createNoteStorage";
import { Note } from "@/generated/backend";
import NoteNewButton from "./NoteNewButton.vue";
import SvgAddChild from "../../svgs/SvgAddChild.vue";
import WikidataButton from "./WikidataButton.vue";
import SvgSearchForLink from "../../svgs/SvgSearchForLink.vue";
import LinkNoteDialog from "../../links/LinkNoteDialog.vue";
import SvgCog from "../../svgs/SvgCog.vue";
import SvgChat from "../../svgs/SvgChat.vue";
import SvgUp from "../../svgs/SvgUp.vue";
import SvgDown from "../../svgs/SvgDown.vue";
import NoteDeleteButton from "./NoteDeleteButton.vue";
import PopButton from "../../commons/Popups/PopButton.vue";
import AIGenerateImageDialog from "../AIGenerateImageDialog.vue";
import NoteDetailsAutoCompletionButton from "./NoteDetailsAutoCompletionButton.vue";
import NoteChatDialog from "../NoteChatDialog.vue";
import NoteInfoBar from "../NoteInfoBar.vue";
import AssessmentDialog from "../AssessmentDialog.vue";

const props = defineProps({
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
});

defineEmits(["note-accessory-updated"]);

const moveUp = () => {
  props.storageAccessor.storedApi().moveUp(props.note.id);
};
const moveDown = () => {
  props.storageAccessor.storedApi().moveDown(props.note.id);
};
</script>
