<template>
    <div class="inner-box" v-if="selectedNote">
      <div class="header">
        <NoteControl v-bind="{selectedNote, selectedNotePosition, viewType}"/>
      </div>
      <div class="content">
        <NoteMindmapView v-if="viewType==='mindmap'"
          v-bind="{noteId, expandChildren}"
          :highlightNoteId="selectedNoteId"
          @selectNote="highlight($event)"
        />
        <NoteArticleView v-if="viewType==='article'"
          v-bind="{noteId, expandChildren}"
        />
        <NoteCardsView v-if="!viewType || viewType==='cards'"
          v-bind="{noteId, expandChildren}"
        />
        <div class="comments" v-if="featureToggle">
          <div v-for="(comment, index) in comments" :key="index">{{index + 1}}-{{comment.description}}</div>
          <input id="reply-input" data-testid="reply-input" name="reply-input" placeholder="Reply..." type="text" @blur="onBlurTextField"/>
        </div>
      </div>
    </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import NoteControl from "../../toolbars/NoteControl.vue";
import NoteMindmapView from "./NoteMindmapView.vue";
import NoteCardsView from "./NoteCardsView.vue";
import NoteArticleView from "./NoteArticleView.vue";
import useStoredLoadingApi from '../../../managedApi/useStoredLoadingApi';


export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  props: {
     noteId: { type: Number, required: true },
     viewType: String,
     expandChildren: { type: Boolean, required: true },
  },
  data() {
    return {
      selectedNoteId: undefined,
      comments: []
    } as {
      selectedNoteId: Doughnut.ID | undefined,
      comments: Comment[]
    }
  },
  components: { NoteControl, NoteMindmapView, NoteCardsView, NoteArticleView },
  methods: {
    highlight(id: Doughnut.ID) {
      this.selectedNoteId = id
    },
    onBlurTextField(event: Event) {
      this.comments.push({description: event.target.value})
    }
  },
  computed: {
    selectedNotePosition(): Generated.NotePositionViewedByUser | undefined {
      if(!this.selectedNoteId) return
      return this.piniaStore.getNotePosition(this.selectedNoteId)
    },
    selectedNote() {
      return this.piniaStore.getNoteRealmById(this.noteId)?.note
    },
    featureToggle() { return this.piniaStore.featureToggle },
  },
  mounted() {
    this.highlight(this.noteId)
  }
});
</script>
