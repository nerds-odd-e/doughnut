<template>
  <div>
    <div v-if="!!noteWithPosition">
      <NoteWithControls
        v-bind="{note, notePosition}"
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
  </div>
</template>

<script setup>
import NoteWithControls from "../notes/NoteWithControls.vue";
import LinkShow from "../links/LinkShow.vue";
import LinkNob from "../links/LinkNob.vue";
import { computed } from "@vue/reactivity";
const props = defineProps({
  noteWithPosition: Object,
  linkViewedByUser: Object,
});

const notePosition = computed(()=>{
  const {notePosition} = props.noteWithPosition
  return notePosition
})

const note = computed(()=>{
  const {noteItself} = props.noteWithPosition
  return noteItself
})

</script>
