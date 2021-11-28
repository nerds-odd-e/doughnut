<template>
    <div class="inner-box">
      <div class="header">
        <NoteControl :noteId="highlightNoteId" :viewType="viewType" @updateLanguage="language=$event"/>
        <Breadcrumb v-bind="notePosition"/>
      </div>
      <div class="content">
        <component :is="noteComponent" 
          v-bind="{noteId, highlightNoteId, expandChildren, language}"
          @highlight="highlight"
        />
      </div>
    </div>
</template>

<script>
import NoteControl from "../../toolbars/NoteControl.vue";
import NoteMindmapView from "./NoteMindmapView.vue";
import NoteCardsView from "./NoteCardsView.vue";
import NoteArticleView from "./NoteArticleView.vue";
import Breadcrumb from "../Breadcrumb.vue";

export default {
  props: {
     noteId: [String, Number],
     notePosition: Object,
     noteComponent: String,
     viewType: String,
     expandChildren: { type: Boolean, required: true },
  },
  data() {
    return {
      highlightNoteId: null,
      language: null,
    };
  },
  components: { NoteControl, NoteMindmapView, Breadcrumb, NoteCardsView, NoteArticleView },
  methods: {
    highlight(id) { this.highlightNoteId = id},
  },
  watch: {
    noteId() {
      this.highlightNoteId = this.noteId
    },
  },
  mounted() {
    this.highlightNoteId = this.noteId
  },
};
</script>
