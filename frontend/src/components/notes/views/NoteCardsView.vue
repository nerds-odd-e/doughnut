<template>
  <div class="container" v-if="noteSphere">
    <NoteWithLinks v-bind="{ note: noteSphere.note, links: noteSphere.links }"/>
    <NoteStatisticsButton :noteId="noteId" />
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
    noteSphere() {
      return this.piniaStore.getNoteSphereById(this.noteId);
    },
    children() {
      return this.piniaStore.getChildrenOfParentId(this.noteId);
    },
  }
});
</script>
