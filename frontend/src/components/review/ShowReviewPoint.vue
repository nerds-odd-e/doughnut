<template>
    <div v-if="!!noteWithPosition">
      <NotePageFrame
      v-bind="{
        noteId,
        notePosition: noteWithPosition.notePosition,
        deleteRedirect: true,
        expendChildren: false,
        viewType: 'cards',
        noteRouteName: 'noteShow',
        noteComponent: 'NoteWithChildrenCards'}"/>
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
import NotePageFrame from "../notes/NotePageFrame.vue";
import LinkShow from "../links/LinkShow.vue";
import LinkNob from "../links/LinkNob.vue";

export default {
  props: {
    noteWithPosition: Object,
    linkViewedByUser: Object,
  },
  components: {NotePageFrame, LinkShow, LinkNob},
  computed: {
    noteId() {
      return this.noteWithPosition.notePosition.noteId
    },
  }
}

</script>