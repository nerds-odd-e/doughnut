<template>
  <LinkLists v-bind="{links, owns, staticInfo}">
    <div class="note-body">
      <h2 :class="'note-title h' + level"> {{note.noteContent.title}}</h2>
      <div class="row">
        <pre :class="`col-12 ${twoColumns ? 'col-md-6' : ''} note-body`" style="white-space: pre-wrap;">{{note.noteContent.description}}</pre>
        <ShowPicture :class="`col-12 ${twoColumns ? 'col-md-6' : ''} note-body`" :note="note" :opacity="0.2"/>
      </div>
      <div v-if="!!note.noteContent.url">
          <label v-if="note.noteContent.urlIsVideo">Video Url:</label>
          <label v-else>Url:</label>
          <a :href="note.noteContent.url">{{note.noteContent.url}}</a>
      </div>
  </div>
</LinkLists>


</template>

<script setup>
import { computed } from "@vue/runtime-core"
import LinkLists from "../links/LinkLists.vue"
import ShowPicture from "./ShowPicture.vue"

const props = defineProps({
    note: {type: Object, required: true },
    links: Object,
    level: { type: Number, default: 2 },
    owns: {type: Boolean, required: true},
    staticInfo: Object })

const twoColumns = computed(()=>!!props.note.notePicture && !!props.note.noteContent.description)
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
  color: v-bind(staticInfo.colors['target']);
}
</style>
