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
  <a
    :title="'Suggest1'"
    class="btn btn-sm"
    role="button"
    @click="suggestDescriptionByTitle"
  >
    <SvgRobot />
  </a>
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
import useLoadingApi from "@/managedApi/useLoadingApi";
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
import SvgRobot from "../svgs/SvgRobot.vue";

export default defineComponent({
  setup() {
    return {
      ...useLoadingApi(),
    };
  },
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
    SvgRobot,
  },
  computed: {
    viewType() {
      return sanitizeViewTypeName(this.$route.meta?.viewType as string);
    },
  },
  methods: {
    generateTextContent(title: string, description: string) {
      return {
        title,
        description,
      };
    },
    async askSuggestionApi(selectedNote: Generated.Note, prompt: string) {
      const res = await this.api.ai.askAiSuggestions({
        prompt,
      });

      await this.storageAccessor
        .api(this.$router)
        .updateTextContent(
          selectedNote.id,
          this.generateTextContent(selectedNote.title, res.suggestion),
          selectedNote.textContent
        );
      if (res.finishReason === "length") {
        await this.askSuggestionApi(selectedNote, res.suggestion);
      }
    },
    async suggestDescriptionByTitle() {
      await this.askSuggestionApi(
        this.selectedNote,
        this.selectedNote.textContent.description
          ?.replace(/<\/?[^>]+(>|$)/g, "")
          .trim() || `Tell me about "${this.selectedNote.title}"`
      );
    },
  },
});
</script>
