<template>
    <div v-if="!!noteWithPosition">
      <CurrentNoteContainer :noteId="noteWithPosition.note.id"/>
      <Breadcrumb v-bind="notePosition" />
      <NoteWithLinks v-bind="noteWithPosition.note"/>
      />
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

<script setup>
import NoteWithLinks from "../notes/NoteWithLinks.vue";
import Breadcrumb from "../notes/Breadcrumb.vue";
import CurrentNoteContainer from "../commons/CurrentNoteContainer.vue";
import LinkShow from "../links/LinkShow.vue";
import LinkNob from "../links/LinkNob.vue";
import { computed } from "@vue/reactivity";

const props = defineProps({
  noteWithPosition: Object,
  linkViewedByUser: Object,
});

</script>
