<template>
  <ViewTypeButtons v-bind="{ viewType, noteId: selectedNote.id }" />
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
  <AISuggestion v-bind="{ selectedNote, storageAccessor }" />
  <AiQuestionGeneration v-bind="{ selectedNote, storageAccessor }" />
  <PopButton title="search and link note">
    <template #button_face>
      <SvgSearch />
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
      <PopButton class="dropdown-item btn-primary" title="Engaging Story">
        <NoteEngagingStoryDialog v-bind="{ selectedNote, storageAccessor }" />
      </PopButton>
      <NoteDeleteButton
        class="dropdown-item"
        v-bind="{ noteId: selectedNote.id, storageAccessor }"
      />
    </div>
  </div>
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
import SvgSearch from "../svgs/SvgSearch.vue";
import LinkNoteDialog from "../links/LinkNoteDialog.vue";
import ViewTypeButtons from "./ViewTypeButtons.vue";
import { sanitizeViewTypeName } from "../../models/viewTypes";
import SvgCog from "../svgs/SvgCog.vue";
import NoteDeleteButton from "./NoteDeleteButton.vue";
import PopButton from "../commons/Popups/PopButton.vue";
import NoteEngagingStoryDialog from "../notes/NoteEngagingStoryDialog.vue";
import AISuggestion from "./AISuggestion.vue";

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
    SvgSearch,
    LinkNoteDialog,
    ViewTypeButtons,
    SvgCog,
    NoteDeleteButton,
    PopButton,
    NoteEngagingStoryDialog,
    AISuggestion,
  },
  computed: {
    viewType() {
      return sanitizeViewTypeName(this.$route.meta?.viewType as string);
    },
  },
});
</script>
