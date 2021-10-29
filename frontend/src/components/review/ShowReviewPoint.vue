<template>
    <div v-if="!!noteWithPosition">
      <CurrentNoteContainer :noteId="noteWithPosition.notePosition.noteId"/>
      <Breadcrumb v-bind="noteWithPosition.notePosition" />
      <NoteWithLinks v-bind="note"/>
    </div>

    <div v-if="!!linkViewedByUser">
      <div class="jumbotron py-4 mb-2">
        <LinkShow v-bind="linkViewedByUser">
          <LinkNob
            v-bind="{ link: linkViewedByUser }"
          />
          <span class="badge bg-light text-dark">
            {{ linkViewedByUser.linkTypeLabel }}</span
          >
        </LinkShow>
      </div>
    </div>
</template>

<script>
import NoteWithLinks from "../notes/NoteWithLinks.vue";
import Breadcrumb from "../notes/Breadcrumb.vue";
import CurrentNoteContainer from "../commons/CurrentNoteContainer.vue";
import LinkShow from "../links/LinkShow.vue";
import LinkNob from "../links/LinkNob.vue";

export default {
  props: {
    noteWithPosition: Object,
    linkViewedByUser: Object,
  },
  components: {CurrentNoteContainer, NoteWithLinks, Breadcrumb, LinkShow, LinkNob},
  computed: {
    note() {
      return this.$store.getters.getNoteById(this.noteWithPosition.notePosition.noteId);
    },
  }
}

</script>
