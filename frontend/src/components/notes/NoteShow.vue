<template>
  <NoteFrameOfLinks v-bind="{ links }" @updated="$emit('updated')">
    <div v-if="recentlyUpdated">This note has been changed recently.</div>
    <div class="note-body">
      <h2 role="title" class="note-title">{{ note.noteContent.title }}</h2>
      <div class="row">
        <ShowDescription
          :class="`col-12 ${twoColumns ? 'col-md-6' : ''}`"
          :description="note.noteContent.description"
        />
        <ShowPicture
          :class="`col-12 ${twoColumns ? 'col-md-6' : ''}`"
          :note="note"
          :opacity="0.2"
        />
      </div>
      <div v-if="!!note.noteContent.url">
        <label v-if="note.noteContent.urlIsVideo">Video Url:</label>
        <label v-else>Url:</label>
        <a :href="note.noteContent.url">{{ note.noteContent.url }}</a>
      </div>
    </div>
  </NoteFrameOfLinks>
</template>

<script setup>
import { computed } from "@vue/runtime-core";
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import ShowPicture from "./ShowPicture.vue";
import ShowDescription from "./ShowDescription.vue";

const props = defineProps({
  id: Number,
  note: { type: Object, required: true },
  links: Object,
  recentlyUpdated: Boolean,
});
const emits = defineEmits(["updated"]);

const twoColumns = computed(
  () => !!props.note.notePicture && !!props.note.noteContent.description
);
</script>

<style scoped>
.note-body {
  background-color: #eee;
  padding-left: 10px;
  padding-right: 10px;
}
.note-title {
  margin-top: 0px;
  padding-top: 10px;
  color: v-bind($staticInfo.colors[ "target" ]);
}

</style>
