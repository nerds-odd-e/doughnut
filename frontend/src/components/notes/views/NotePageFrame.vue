<template>
    <div class="inner-box">
      <div class="header">
        <Breadcrumb v-bind="notePosition"/>
      </div>
      <div class="content">
        <component :is="noteComponent" 
          v-bind="{noteId, expandChildren}"
          @on-editing="onEditing"
        />
      </div>
    </div>
</template>

<script>
import NoteMindmapView from "./NoteMindmapView.vue";
import NoteCardsView from "./NoteCardsView.vue";
import NoteArticleView from "./NoteArticleView.vue";
import Breadcrumb from "../Breadcrumb.vue";

export default {
  props: {
     noteId: [String, Number],
     notePosition: Object,
     noteComponent: String,
     expandChildren: { type: Boolean, required: true },
  },
  components: { NoteMindmapView, Breadcrumb, NoteCardsView, NoteArticleView },
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
