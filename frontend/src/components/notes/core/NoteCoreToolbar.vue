<template>
  <NoteRecentUpdateIndicator
    v-bind="{ id: note.id, updatedAt: note.updatedAt }"
  >
    <nav class="navbar justify-content-between">
      <div class="btn-group btn-group-sm">
        <NoteNewButton
          button-title="Add Child Note"
          v-bind="{ parentId: note.id, storageAccessor }"
        >
          <SvgAddChild />
        </NoteNewButton>

        <WikidataButton v-bind="{ note, storageAccessor }" />
        <NoteDetailsAutoCompletionButton v-bind="{ note, storageAccessor }" />
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
              title="Generate Image with DALL-E"
            >
              <AIGenerateImageDialog v-bind="{ note, storageAccessor }" />
            </PopButton>
            <NoteDeleteButton
              class="dropdown-item"
              v-bind="{ noteId: note.id, storageAccessor }"
            />
          </div>
        </div>
      </div>
    </nav>
  </NoteRecentUpdateIndicator>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { StorageAccessor } from "@/store/createNoteStorage";
import { Note } from "@/generated/backend";
import NoteNewButton from "./NoteNewButton.vue";
import SvgAddChild from "../../svgs/SvgAddChild.vue";
import WikidataButton from "./WikidataButton.vue";
import SvgSearchForLink from "../../svgs/SvgSearchForLink.vue";
import LinkNoteDialog from "../../links/LinkNoteDialog.vue";
import SvgCog from "../../svgs/SvgCog.vue";
import NoteDeleteButton from "./NoteDeleteButton.vue";
import PopButton from "../../commons/Popups/PopButton.vue";
import AIGenerateImageDialog from "../AIGenerateImageDialog.vue";
import NoteDetailsAutoCompletionButton from "./NoteDetailsAutoCompletionButton.vue";
import NoteRecentUpdateIndicator from "./NoteRecentUpdateIndicator.vue";

export default defineComponent({
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    note: {
      type: Object as PropType<Note>,
      required: true,
    },
  },
  components: {
    NoteNewButton,
    SvgAddChild,
    WikidataButton,
    SvgSearchForLink,
    LinkNoteDialog,
    SvgCog,
    NoteDeleteButton,
    PopButton,
    AIGenerateImageDialog,
    NoteRecentUpdateIndicator,
    NoteDetailsAutoCompletionButton,
  },
});
</script>
