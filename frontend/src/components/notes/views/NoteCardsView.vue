<template>
  <div class="container" v-if="noteRealm">
    <NoteWithLinks v-bind="{ note: noteRealm.note, links: noteRealm.links }"/>
    <NoteStatisticsButton :noteId="noteId" />

    <input id="comment-input" />

    <Cards v-if="expandChildren" :notes="children"/>
  </div>

</template>

<script lang="ts">
import { defineComponent } from 'vue'
import NoteWithLinks from "../NoteWithLinks.vue";
import NoteStatisticsButton from "../NoteStatisticsButton.vue";
import Cards from "../Cards.vue";
import useStoredLoadingApi from "../../../managedApi/useStoredLoadingApi";


export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  props: {
    noteId: { type: Number, required: true },
    expandChildren: { type: Boolean, required: true },
  },
  components: {
    NoteWithLinks,
    Cards,
    NoteStatisticsButton,
  },
  computed: {
    noteRealm() {
      return this.piniaStore.getNoteRealmById(this.noteId);
    },
    children() {
      return this.noteRealm?.childrenIds
        ?.map((id: Doughnut.ID)=>this.piniaStore.getNoteRealmById(id)?.note)
        .filter((n)=>n)
    },
  }
});
</script>
