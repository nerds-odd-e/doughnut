<template>
  <NoteFrameOfLinks v-bind="{ links }" @updated="$emit('updated')">
    <div v-if="recentlyUpdated">This note has been changed recently.</div>
    <div class="note-body" :style="`background-color: ${bgColor}`">
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

const bgColor = computed(
  () => {
    const colorOld = [96, 96, 96]
    const newColor = [208, 237, 23]
    const ageInMillisecond = new Date() - new Date(props.note.noteContent.updatedAt)
    const max = 15 // equals to 225 hours
    const index = Math.min(max, Math.sqrt(ageInMillisecond / 1000 / 60 / 60))
    return `rgb(${colorOld.map((oc, i)=>(oc * index + newColor[i] * (max-index))/max).join(',')})`
  }
);

</script>

<style scoped>
.note-body {
  padding-left: 10px;
  padding-right: 10px;
}
.note-body[data-age] {
  background-color:rgb(200 * attr(data-age)/10, 200 * attr(data-age)/10, 200 * attr(data-age)/10);
}
.note-title {
  margin-top: 0px;
  padding-top: 10px;
  color: v-bind($staticInfo.colors[ "target" ]);
}
</style>
