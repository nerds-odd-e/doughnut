<template>
  <div class="btn-group btn-group-sm">
    <template v-if="!selectedNote">
      <PopButton title="link note">
        <template #button_face>
          <SvgSearch />
        </template>
        <LinkNoteDialog v-bind="{ storageAccessor }" />
      </PopButton>
    </template>
    <template v-if="selectedNote">
      <ViewTypeButtons v-bind="{ viewType, noteId: selectedNote.id }" />
      <NoteNewButton
        v-bind="{ parentId: selectedNote.id, storageAccessor }"
        button-title="Add Child Note"
      >
        <SvgAddChild />
      </NoteNewButton>

      <PopButton title="edit note">
        <template #button_face>
          <SvgEdit />
        </template>
        <NoteEditDialog v-bind="{ note: selectedNote, storageAccessor }" />
      </PopButton>

      <PopButton title="associate wikidata">
        <template #button_face>
          <SvgWikidata />
        </template>
        <WikidataAssociationDialog
          v-bind="{ note: selectedNote, storageAccessor }"
        />
      </PopButton>

      <PopButton title="link note">
        <template #button_face>
          <SvgSearch />
        </template>
        <LinkNoteDialog v-bind="{ note: selectedNote, storageAccessor }" />
      </PopButton>
      <div class="dropdown">
        <button
          class="btn dropdown-toggle"
          id="dropdownMenuButton"
          data-bs-toggle="dropdown"
          aria-haspopup="true"
          aria-expanded="false"
          role="button"
          title="more options"
        >
          <SvgCog />
        </button>
        <div class="dropdown-menu dropdown-menu-end">
          <PopButton class="dropdown-item btn-primary" title="Engaging Story">
            <NoteEngagingStoryDialog
              v-bind="{ selectedNoteId: selectedNote.id, storageAccessor }"
            />
          </PopButton>
          <NoteDeleteButton
            class="dropdown-item"
            v-bind="{ noteId: selectedNote.id, storageAccessor }"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteNewButton from "./NoteNewButton.vue";
import SvgAddChild from "../svgs/SvgAddChild.vue";
import SvgEdit from "../svgs/SvgEdit.vue";
import NoteEditDialog from "../notes/NoteEditDialog.vue";
import SvgWikidata from "../svgs/SvgWikidata.vue";
import WikidataAssociationDialog from "../notes/WikidataAssociationDialog.vue";
import SvgSearch from "../svgs/SvgSearch.vue";
import LinkNoteDialog from "../links/LinkNoteDialog.vue";
import ViewTypeButtons from "./ViewTypeButtons.vue";
import { sanitizeViewTypeName } from "../../models/viewTypes";
import SvgCog from "../svgs/SvgCog.vue";
import NoteDeleteButton from "./NoteDeleteButton.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import PopButton from "../commons/Popups/PopButton.vue";
import NoteEngagingStoryDialog from "../notes/NoteEngagingStoryDialog.vue";

export default defineComponent({
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    user: { type: Object as PropType<Generated.User> },
  },
  emits: ["updateUser"],
  components: {
    NoteNewButton,
    SvgAddChild,
    SvgEdit,
    NoteEditDialog,
    SvgWikidata,
    WikidataAssociationDialog,
    SvgSearch,
    LinkNoteDialog,
    ViewTypeButtons,
    SvgCog,
    NoteDeleteButton,
    PopButton,
    NoteEngagingStoryDialog,
  },
  computed: {
    selectedNote(): Generated.Note | undefined {
      return this.storageAccessor.selectedNote;
    },
    viewType() {
      return sanitizeViewTypeName(this.$route.meta.viewType as string);
    },
  },
});
</script>
