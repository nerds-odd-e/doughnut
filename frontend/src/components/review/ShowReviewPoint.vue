<template>
    <div v-if="!!noteWithPosition">
    <div class="box">
      <div class="header">
      <NoteControl :noteId="noteId" :deleteRedirect="true"/>
      <Breadcrumb v-bind="noteWithPosition.notePosition" />
      </div>
      <div class="content">
      <NoteWithLinks v-bind="note" v-if="note"/>
      </div>
    </div>
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
import NoteControl from "../commons/NoteControl.vue";
import LinkShow from "../links/LinkShow.vue";
import LinkNob from "../links/LinkNob.vue";

export default {
  props: {
    noteWithPosition: Object,
    linkViewedByUser: Object,
  },
  components: {NoteControl, NoteWithLinks, Breadcrumb, LinkShow, LinkNob},
  computed: {
    noteId() {
      return this.noteWithPosition.notePosition.noteId
    },
    note() {
      return this.$store.getters.getNoteById(this.noteId)
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