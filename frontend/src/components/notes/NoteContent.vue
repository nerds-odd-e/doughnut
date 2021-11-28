<template>
  <div class="note-content">
    <template v-if="!!translatedDescription">
      <ShowDescription
        v-if="size==='large'"
        class="col"
        :description="translatedDescription"
      />
      <NoteShortDescription class="col" v-if="size==='medium'" :shortDescription="translatedShortDescription"/>
      <SvgDescriptionIndicator v-if="size==='small'" class="description-indicator"/>
    </template>
    <template v-if="!!note.notePicture">
      <ShowPicture
        v-if="size!=='small'"
        class="col text-center"
        v-bind="{notePicture: note.notePicture, pictureMask: note.noteContent.pictureMask}"
        :opacity="0.2"
      />
      <SvgPictureIndicator v-else class="picture-indicator"/>
    </template>
    <template v-if="!!note.noteContent.url">
      <div class="col" v-if="size!='small'">
        <label v-if="note.noteContent.urlIsVideo">Video Url:</label>
        <label v-else>Url:</label>
        <a :href="note.noteContent.url" target="_blank">{{ note.noteContent.url }}</a>
      </div>
      <a v-else :href="note.noteContent.url" target="_blank">
        <SvgUrlIndicator/>
      </a>
    </template>
  </div>
</template>

<script>
import NoteShortDescription from "./NoteShortDescription.vue";
import ShowPicture from "./ShowPicture.vue";
import ShowDescription from "./ShowDescription.vue";
import SvgDescriptionIndicator from "../svgs/SvgDescriptionIndicator.vue";
import SvgPictureIndicator from "../svgs/SvgPictureIndicator.vue";
import SvgUrlIndicator from "../svgs/SvgUrlIndicator.vue";
import { NoteWrapper } from "../../constants/lang";

export default {
  props: {
    note: Object,
    size: { type: String, default: 'large'},
    language: String,
  },
  components: {
    NoteShortDescription,
    ShowPicture,
    ShowDescription,
    SvgDescriptionIndicator,
    SvgPictureIndicator,
    SvgUrlIndicator,
  },
  computed: {
    twoColumns() {
      return !!this.notePicture && !!this.note.noteContent.description;
    },
    translatedDescription(){
      return new NoteWrapper(this.note).translatedDescription(this.language);
    },
    translatedShortDescription(){
      return new NoteWrapper(this.note).translatedShortDescription(this.language);
    }
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