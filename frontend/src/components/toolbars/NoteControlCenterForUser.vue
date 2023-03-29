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
        v-if="environment === 'testing'"
        class="btn btn-sm"
        role="button"
        :title="'Suggest1'"
        @click="suggestDescriptionByTitle"
      >
        <SvgRobot />
      </a>
      <PopButton title="Suggest">
        <template #button_face>
          <SvgRobot />
        </template>
        <NoteSuggestDescriptionDialog
          v-bind="{ selectedNote, storageAccessor }"
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
              v-bind="{ selectedNote, storageAccessor }"
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
import NoteSuggestDescriptionDialog from "../notes/NoteSuggestDescriptionDialog.vue";

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
    user: { type: Object as PropType<Generated.User> },
  },
  emits: ["updateUser"],
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
    NoteSuggestDescriptionDialog,
  },
  data() {
    return {
      environment: "production",
    };
  },
  computed: {
    selectedNote(): Generated.Note | undefined {
      return this.storageAccessor.selectedNote;
    },
    viewType() {
      return sanitizeViewTypeName(this.$route.meta.viewType as string);
    },
  },
  methods: {
    suggestDescriptionByTitle() {
      const { selectedNote } = this.storageAccessor;
      if (selectedNote) {
        this.api.ai
          .askAiSuggestions({ prompt: `Tell me about "${selectedNote.title}"` })
          .then((res: Generated.AiSuggestion) => {
            this.storageAccessor.api(this.$router).updateTextContent(
              selectedNote.id,
              {
                title: selectedNote.title,
                description: res.suggestion,
                updatedAt: new Date().toDateString(),
              },
              selectedNote.textContent
            );
          });
      }
    },
  },
  async mounted() {
    this.environment = await this.api.testability.getEnvironment();
  },
});
</script>
