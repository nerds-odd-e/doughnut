<template>
    <div v-if="noteWithPosition">
      <NotePageFrame
      v-if="noteId"
      v-bind="{
        noteId,
        notePosition: noteWithPosition.notePosition,
        expandChildren: false,
        noteComponent: 'NoteCardsView'}"/>
    </div>

    <div v-if="linkViewedByUser">
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

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NotePageFrame from "../notes/views/NotePageFrame.vue";
import LinkShow from "../links/LinkShow.vue";
import LinkNob from "../links/LinkNob.vue";
import LinkViewedByUserBuilder from "../../../tests/fixtures/LinkViewedByUserBuilder";

export default defineComponent({
  props: {
    noteWithPosition: Object as PropType<Generated.NoteWithPosition>,
    linkViewedByUser: Object as PropType<LinkViewedByUserBuilder>,
  },
  components: {NotePageFrame, LinkShow, LinkNob},
  computed: {
    noteId() {
      return this.noteWithPosition?.note.id
    },
  }
})

</script>