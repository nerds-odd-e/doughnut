<template>
    <div v-if="!!noteWithPosition">
      <NotePageFrame
      v-bind="{
        noteId,
        notePosition,
        deleteRedirect: true,
        expendChildren: false,
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

<style lang="sass" scoped>
.box
  display: flex
  flex-flow: column
  height: 100%

.box .header
  flex: 0 1 auto

.box .content
  flex: 1 1 auto
  overflow: hidden

.box .footer
  flex: 0 1 40px
</style>