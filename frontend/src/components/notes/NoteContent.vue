<template>
  <div class="note-content">
        <template v-if="!!translatedNote.description || isEditingDescription">
      <div id="description-id" class="note-content" @click="onDescriptionClick" v-if="!isEditingDescription">
        <ShowDescription
          v-if="size==='large'"
          class="col"
          :description="translatedNote.description"
        />
        <NoteShortDescription class="col" v-if="size==='medium'" :shortDescription="translatedNote.shortDescription"/>
        <SvgDescriptionIndicator v-if="size==='small'" class="description-indicator"/>
      </div>
      <TextArea class="note-content-description" id="description-form-id" scopeName="note" v-model="translatedNote.description" :autofocus="true" 
        @blur="onBlurTextField" v-if="isEditingDescription" v-on:keydown.enter.shift="$event.target.blur()"/>
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
import ShowDescription from "./ShowDescription.vue";
import SvgDescriptionIndicator from "../svgs/SvgDescriptionIndicator.vue";
import SvgPictureIndicator from "../svgs/SvgPictureIndicator.vue";
import SvgUrlIndicator from "../svgs/SvgUrlIndicator.vue";
import { TranslatedNoteWrapper } from "../../models/languages";
import TextArea from "../form/TextArea.vue";

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
    ShowDescription,
    SvgDescriptionIndicator,
    SvgPictureIndicator,
    SvgUrlIndicator,
    TextArea
  },
  data() {
    return {
      isEditingDescription: false,
    };
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
    onDescriptionClick() {
      this.isEditingDescription = true;
    },
    onBlurTextField() {
      this.isEditingDescription = false;
      this.$emit("blur", {description: this.note.noteContent.description});
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



.note-content-description
  width: 100%
  display: inline

</style>
