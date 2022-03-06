<template>
    <div class="inner-box">
      <div class="header">
        <NoteControl/>
        <Breadcrumb v-bind="notePosition"/>
      </div>
      <div class="content">
        <component :is="noteComponent" 
          v-bind="{noteId, expandChildren}"
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
     expandChildren: { type: Boolean, required: true },
  },
  components: { NoteControl, NoteMindmapView, Breadcrumb, NoteCardsView, NoteArticleView },
  emits: ['highlight'],
  methods: {
    highlight(id) { 
      this.$emit('highlight')
    },
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
