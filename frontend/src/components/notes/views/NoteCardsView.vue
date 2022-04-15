<template>
  <div class="container" v-if="noteRealm">
    <NoteWithLinks v-bind="{ note: noteRealm.note, links: noteRealm.links }"/>
    <NoteStatisticsButton :noteId="noteId" />

    <div v-if="featureToggle">
      <div v-for="item in noteRealm.note.comments" :key="item.id" class="comment">
        {{ item.author.name }}: {{ item.description }} <div class="comment-timestamp">{{ item.createdAt }}</div><br/>
        <button :id="`comment-${item.id}-delete`" @click="deleteComment(item.id)">Delete</button>
      </div>
      <input  id="comment-input" @blur="handleBlur" v-model="creationData.description"/>
    </div>

    <Cards v-if="expandChildren" :notes="children"/>
  </div>

</template>

<script lang="ts">
import {defineComponent} from 'vue'
import NoteWithLinks from "../NoteWithLinks.vue";
import NoteStatisticsButton from "../NoteStatisticsButton.vue";
import Cards from "../Cards.vue";
import useStoredLoadingApi from "../../../managedApi/useStoredLoadingApi";

function initialState() {
  return {
    creationData: {
      description: ""
    },
  };
}

export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  props: {
    noteId: { type: Number, required: true },
    expandChildren: { type: Boolean, required: true },
  },
  data(){
    return {
      ...initialState(),
    };
  },
  components: {
    NoteWithLinks,
    Cards,
    NoteStatisticsButton,
  },
  methods:{
    handleBlur(){
      this.storedApi.addComment(this.noteId, this.creationData)
    },
    deleteComment(commentId: number) {
      this.storedApi.deleteComment(this.noteId, commentId)
    } 
  },
  computed: {
    noteRealm() {
      return this.piniaStore.getNoteRealmById(this.noteId);
    },
    featureToggle() { return this.piniaStore.featureToggle },
    children() {
      return this.noteRealm?.childrenIds
        ?.map((id: Doughnut.ID)=>this.piniaStore.getNoteRealmById(id)?.note)
        .filter((n)=>n)
    },
  }
});
</script>
