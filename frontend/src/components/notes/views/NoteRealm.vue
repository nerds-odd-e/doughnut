<template>
  <div class="inner-box">
    <div class="header">
      <NoteToolbar v-bind="{ selectedNote, selectedNotePosition, viewType }" />
    </div>
    <div class="content" v-if="noteRealm">
      <NoteMindmapView
        v-if="viewType === 'mindmap'"
        v-bind="{ noteId, expandChildren }"
        :highlight-note-id="selectedNoteId"
        @selectNote="highlight($event)"
      />
      <div class="container" v-if="viewType === 'article'">
        <NoteArticleView v-bind="{ noteRealm, expandChildren }" />
      </div>
      <NoteCardsView
        v-if="!viewType || viewType === 'cards'"
        v-bind="{ noteRealm, expandChildren, comments }"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteToolbar from "../../toolbars/NoteToolbar.vue";
import NoteMindmapView from "./NoteMindmapView.vue";
import NoteCardsView from "./NoteCardsView.vue";
import NoteArticleView from "./NoteArticleView.vue";
import useStoredLoadingApi from "../../../managedApi/useStoredLoadingApi";

export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  props: {
    noteId: { type: Number, required: true },
    noteRealm: {
      type: Object as PropType<Generated.NoteRealm>,
      required: true,
    },
    viewType: String,
    expandChildren: { type: Boolean, required: true },
    comments: {
      type: Object as PropType<Generated.Comment[]>,
      default: () => [] as Generated.Comment[],
    },
  },
  data() {
    return {
      selectedNoteId: undefined,
    } as {
      selectedNoteId: Doughnut.ID | undefined;
    };
  },
  components: { NoteToolbar, NoteMindmapView, NoteCardsView, NoteArticleView },
  methods: {
    highlight(id: Doughnut.ID) {
      this.selectedNoteId = id;
    },
  },
  computed: {
    selectedNotePosition(): Generated.NotePositionViewedByUser | undefined {
      if (!this.selectedNoteId) return;
      return this.piniaStore.getNotePosition(this.selectedNoteId);
    },
    selectedNote() {
      return this.noteRealm?.note;
    },
  },
  mounted() {
    this.highlight(this.noteId);
  },
});
</script>
