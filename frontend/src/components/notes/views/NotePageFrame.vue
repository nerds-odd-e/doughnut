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
          @on-editing="onEditing"
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
  computed: {
    highlightNoteId() {return this.$store.getters.getHighlightNoteId()},

  },
  methods: {
    highlight(id) { 
      this.$store.commit("highlightNoteId", id)
    },
    onEditing(value){
      this.$emit("on-editing", value);
    }
  },
  watch: {
    noteId() {
      this.highlight(this.noteId)
    },
  },
  mounted() {
    this.highlight(this.noteId)
  }
};
</script>
