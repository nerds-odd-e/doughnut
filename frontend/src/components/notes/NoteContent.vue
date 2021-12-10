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
import Languages, { TranslatedNoteWrapper } from "../../models/languages";
import TextArea from "../form/TextArea.vue";
import { restPatchMultiplePartForm } from "../../restful/restful";

export default {
  props: {
    note: Object,
    size: { type: String, default: 'large'},
    language: String,
    isInPlaceEditEnabled: Boolean,
    isEditingTitle: Boolean,
    isEditingDescription: Boolean,
  },
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
      if (this.isInPlaceEditEnabled && !this.isEditingTitle) {
        this.isEditingDescription = true;
      }
    },
    inputHandler(e) {
      if (e.keyCode === 13 && !e.shiftKey) {
        e.preventDefault();
        this.onBlurTextField();
      }
    },
    onBlurTextField(input) {
      this.isEditingDescription = false;

      const resolvedLanguage = this.language ?? Languages.EN;

      if (resolvedLanguage === Languages.EN) {
        this.note.description = input.target.value;
        this.note.noteContent.description = input.target.value;
        this.note.descriptionIDN = this.note.noteContent.descriptionIDN;
      } else if (resolvedLanguage === Languages.ID) {
        this.note.descriptionIDN = input.target.value;
        this.note.noteContent.descriptionIDN = input.target.value;
        this.note.description = this.note.noteContent.description;
      }

      // Need to update title to make sure we're calling API with correct value.
      this.note.title = this.note.noteContent.title;
      this.note.titleIDN = this.note.noteContent.titleIDN;

      restPatchMultiplePartForm(
        `/api/notes/${this.note.id}`,
        this.note,
        (r) => (this.loading = r)
      )
        .then((res) => {
          this.$store.commit("loadNotes", [res]);
          this.$emit("done");
        })
        .catch((res) => (this.formErrors = res));
    }
  }
};
</script>

<style lang="sass" scoped>
.note-content
  display: flex
  flex-wrap: wrap
.note-content-description
  width: 100%
  display: inline

  .col
    flex: 1 1 auto
    width: 50%



</style>