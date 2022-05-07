<template>
  <div class="container" v-if="noteRealm">
    <NoteWithLinks
      v-bind="{ note: noteRealm.note, links: noteRealm.links }"
      @note-realm-updated="$emit('noteRealmUpdated', $event)"
    />
    <NoteStatisticsButton :note-id="noteRealm.id" />
    <Comments v-bind="{ noteId: noteRealm.id, comments }" />

    <Cards v-if="expandChildren" :notes="noteRealm.children" />
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
  emits: ["noteRealmUpdated"],
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
  },
});
</script>
