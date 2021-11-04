<template>
  <div class="note-content">
    <template v-if="!!note.noteContent.description">
      <ShowDescription
        v-if="size==='large'"
        class="col"
        :description="note.noteContent.description"
      />
      <NoteShortDescription class="col" v-if="size==='medium'" :shortDescription="note.shortDescription"/>
      <SvgDescriptionIndicator v-if="size==='small'" class="description-indicator"/>
    </template>
      <ShowPicture
        v-if="!!note.notePicture"
        class="col text-center"
        v-bind="{notePicture: note.notePicture, pictureMask: note.noteContent.pictureMask}"
        :opacity="0.2"
      />
      <div class="col" v-if="!!note.noteContent.url">
        <label v-if="note.noteContent.urlIsVideo">Video Url:</label>
        <label v-else>Url:</label>
        <a :href="note.noteContent.url">{{ note.noteContent.url }}</a>
      </div>
  </div>
</template>

<script>
import NoteShortDescription from "./NoteShortDescription.vue";
import ShowPicture from "./ShowPicture.vue";
import ShowDescription from "./ShowDescription.vue";
import SvgDescriptionIndicator from "../svgs/SvgDescriptionIndicator.vue";

export default {
  props: {
    note: Object,
    size: { type: String, default: 'large'},
  },
  components: {
    NoteShortDescription,
    ShowPicture,
    ShowDescription,
    SvgDescriptionIndicator,
  },
  computed: {
    twoColumns() {
      return !!this.notePicture && !!this.note.noteContent.description;
    },
  },
};
</script>

<style lang="sass" scoped>
.note-content
  display: flex
  flex-wrap: wrap

  .col
    flex: 1 1 auto
    width: 50%


</style>