<template>
  <template v-if="note">
    <NoteWithLinks v-bind="note"/>
    <NoteOwnerViewCards
      :owns="owns"
      :notes="children"
    />

    <router-link
      :to="{ name: 'noteOverview', params: { noteId: id } }"
      role="button"
      class="btn btn-sm"
    >
      Full view mode
    </router-link>

    <router-link
      :to="{ name: 'mindmap', params: { noteId: id } }"
      role="button"
      class="btn btn-sm"
    >
      Mindmap mode
    </router-link>
  </template>

</template>

<script>
import NoteOwnerViewCards from "./NoteOwnerViewCards.vue";
import NoteWithLinks from "./NoteWithLinks.vue";

export default {
  name: "NoteViewedByUser",
  props: {
    id: [String, Number],
    owns: Boolean,
  },
  components: {
    NoteWithLinks,
    NoteOwnerViewCards,
  },
  computed: {
    note() {
      return this.$store.getters.getNoteById(this.id);
    },
    children() {
      return this.$store.getters.getChildrenOfParentId(this.id);
    },
  },
};
</script>
