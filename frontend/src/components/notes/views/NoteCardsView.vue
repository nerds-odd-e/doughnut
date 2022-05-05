<template>
  <div class="container" v-if="noteRealm">
    <NoteWithLinks v-bind="{ note: noteRealm.note, links: noteRealm.links }" />
    <NoteStatisticsButton :note-id="noteId" />
    <Comments v-bind="{ noteId, comments }" />

    <Cards v-if="expandChildren" :notes="children" />
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteWithLinks from "../NoteWithLinks.vue";
import NoteStatisticsButton from "../NoteStatisticsButton.vue";
import Cards from "../Cards.vue";
import Comments from "../Comments.vue";
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
    expandChildren: { type: Boolean, required: true },
    comments: {
      type: Object as PropType<Generated.Comment[]>,
      default: () => [] as Generated.Comment[],
    },
  },
  components: {
    NoteWithLinks,
    Cards,
    NoteStatisticsButton,
    Comments,
  },

  computed: {
    featureToggle() {
      return this.piniaStore.featureToggle;
    },
    children() {
      return this.noteRealm?.children
        ?.map((id: Doughnut.ID) => this.piniaStore.getNoteRealmById(id)?.note)
        .filter((n) => n);
    },
  },
});
</script>
