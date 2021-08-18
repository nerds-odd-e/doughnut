<template>
  <LinkLists v-bind="{links, owns, colors}">
    <div>
      <h2 :class="'note-title h' + level"> {{note.noteContent.title}}</h2>
      <div :class="`grid grid-cols-1 ${!note.notePicture ? '' : 'md:grid-cols-6'}`">
        <pre class="note-body" style="white-space: pre-wrap;">{{note.noteContent.description}}</pre>
        <ShowPicture :note="note" :opacity="0.2"/>
      </div>
      <div v-if="!!note.noteContent.url">
          <label v-if="note.noteContent.urlIsVideo">Video Url:</label>
          <label v-else>Url:</label>
          <a :href="note.noteContent.url">{{note.noteContent.url}}</a>
      </div>
  </div>
</LinkLists>


</template>

<script>
import LinkLists from "../links/LinkLists.vue"
import ShowPicture from "./ShowPicture.vue"

export default {
  name: "NoteShow",
  props: {
    note: {type: Object, required: true },
    links: Object,
    level: { type: Number, default: 2 },
    owns: {type: Boolean, required: true},
    colors: Object },
  components: { LinkLists, ShowPicture }

}
</script>

<style scoped>
.note-title {
    color: v-bind(colors['target']);
}
</style>
