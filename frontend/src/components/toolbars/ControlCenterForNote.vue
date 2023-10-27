<template>
  <ToolbarFrame>
    <div class="btn-group btn-group-sm">
      <NoteNewButton
        button-title="Add Child Note"
        v-bind="{ parentId: selectedNote.id, storageAccessor }"
      >
        <SvgAddChild />
      </NoteNewButton>

      <PopButton title="edit note">
        <template #button_face>
          <SvgEdit />
        </template>
        <NoteEditAccessoriesDialog
          v-bind="{ note: selectedNote, storageAccessor }"
        />
      </PopButton>

      <PopButton title="associate wikidata">
        <template #button_face>
          <SvgWikidata />
        </template>
        <WikidataAssociationDialog
          v-bind="{ note: selectedNote, storageAccessor }"
        />
      </PopButton>
      <AISuggestDetailsButton v-bind="{ selectedNote, storageAccessor }" />
      <PopButton title="search and link note">
        <template #button_face>
          <SvgSearchForLink />
        </template>
        <LinkNoteDialog v-bind="{ note: selectedNote, storageAccessor }" />
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
            class="dropdown-item btn-primary"
            title="Generate Image with DALL-E"
          >
            <AIGenerateImageDialog v-bind="{ selectedNote, storageAccessor }" />
          </PopButton>
          <NoteDeleteButton
            class="dropdown-item"
            v-bind="{ noteId: selectedNote.id, storageAccessor }"
          />
        </div>
      </div>
    </div>
  </ToolbarFrame>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { StorageAccessor } from "@/store/createNoteStorage";
import NoteNewButton from "./NoteNewButton.vue";
import SvgAddChild from "../svgs/SvgAddChild.vue";
import SvgEdit from "../svgs/SvgEdit.vue";
import NoteEditAccessoriesDialog from "../notes/NoteEditAccessoriesDialog.vue";
import SvgWikidata from "../svgs/SvgWikidata.vue";
import WikidataAssociationDialog from "../notes/WikidataAssociationDialog.vue";
import SvgSearchForLink from "../svgs/SvgSearchForLink.vue";
import LinkNoteDialog from "../links/LinkNoteDialog.vue";
import SvgCog from "../svgs/SvgCog.vue";
import NoteDeleteButton from "./NoteDeleteButton.vue";
import PopButton from "../commons/Popups/PopButton.vue";
import AIGenerateImageDialog from "../notes/AIGenerateImageDialog.vue";
import AISuggestDetailsButton from "./AISuggestDetailsButton.vue";

export default defineComponent({
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    selectedNote: {
      type: Object as PropType<Generated.Note>,
      required: true,
    },
  },
  components: {
    NoteNewButton,
    SvgAddChild,
    SvgEdit,
    NoteEditAccessoriesDialog,
    SvgWikidata,
    WikidataAssociationDialog,
    SvgSearchForLink,
    LinkNoteDialog,
    SvgCog,
    NoteDeleteButton,
    PopButton,
    AIGenerateImageDialog,
    AISuggestDetailsButton,
  },
});
</script>
