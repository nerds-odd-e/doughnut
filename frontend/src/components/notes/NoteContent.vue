<template>
  <div class="note-content">
      <EditableTextArea
          role="description"
          v-if="size==='large'"
          class="col note-description"
          scopeName="note"
          v-model="translatedNote.description"
          @blur="onBlurTextField"/>
      <NoteShortDescription class="col" v-if="size==='medium'" :shortDescription="translatedNote.shortDescription"/>
      <SvgDescriptionIndicator v-if="size==='small' && !!translatedNote.description" class="description-indicator"/>
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
      <div v-if="size!='small'">
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
import SvgDescriptionIndicator from "../svgs/SvgDescriptionIndicator.vue";
import SvgPictureIndicator from "../svgs/SvgPictureIndicator.vue";
import SvgUrlIndicator from "../svgs/SvgUrlIndicator.vue";
import { TranslatedNoteWrapper } from "../../models/languages";
import EditableTextArea from "../form/EditableTextArea.vue";

export default {
  props: {
    note: Object,
    size: { type: String, default: 'large'},
    language: String,
  },
  emits: ['blur'],
  components: {
    NoteShortDescription,
    ShowPicture,
    SvgDescriptionIndicator,
    SvgPictureIndicator,
    SvgUrlIndicator,
    EditableTextArea
  },
  computed: {
    twoColumns() {
      return !!this.notePicture && !!this.note.noteContent.description;
    },
    translatedNote(){
      return new TranslatedNoteWrapper(this.note, this.language);
    },
  },
  methods: {
    onBlurTextField(data) {
      this.$emit("blur", data);
    }
  }
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
