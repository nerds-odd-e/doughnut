<template>
  <NoteShell
    class="note-body"
    v-bind="{ id: note.id, updatedAt: note.textContent?.updatedAt }"
  >
    <NoteFrameOfLinks v-bind="{ links: note.links }">
      <EditableText role="title" class="note-title"
        :multipleLine="false"
        scopeName="note" v-model="translatedNote.title"  v-on="inputListeners"   @blur="submitChange"
      />
      <span 
        role="outdated-tag" 
        class="outdated-label" 
        v-if="translatedNote.isTranslationOutdatedIDN"
        >
          Outdated translation
      </span>
      <p
        style="color: red"
        role="title-fallback"
        v-if="translatedNote.translationNoteAvailable"
      >
        No translation available
      </p>
      <NoteContent v-bind="{ note, language }" v-on="inputListeners"  @blur="submitChange" />
    </NoteFrameOfLinks>
  </NoteShell>
</template>

<script>
import EditableText from "../form/EditableText.vue";
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import NoteShell from "./NoteShell.vue";
import NoteContent from "./NoteContent.vue";
import Languages, { TranslatedNoteWrapper } from "../../models/languages";
import { storedApiUpdateTextContent } from "../../storedApi";
import storeUndoCommand from "../../storeUndoCommand";

export default {
  name: "NoteWithLinks",
  props: {
    note: Object,
    language: String,
  },
  components: {
    NoteFrameOfLinks,
    NoteShell,
    NoteContent,
    EditableText,
  },
  data() {
    return {
      formErrors: {},
    };
  },
  emits:['on-editing'],
  computed: {
    translatedNote(){
      return new TranslatedNoteWrapper(this.note, this.language);
    },
    inputListeners: function () {
      var vm = this;
      return Object.assign({},
        this.$listeners,
        {
          input: function (event) {
            vm.$emit("on-editing", "onEditing");
          }
        }
      )
    }
  },
  methods: {
    submitChange() {
      this.loading = true
      const textContent = (()=>{
        if (this.language === Languages.ID) {
          return {...this.note.translationTextContent, language: 'idn', };
        }
        return this.note.textContent;
      })();
      storeUndoCommand.addUndoHistory(this.$store,  {id: this.note.id, textContent: textContent});
      storedApiUpdateTextContent(this.$store, this.note.id, textContent)
      .then((res) => {
        this.$emit("done");
      })
      .catch((res) => (this.formErrors = res))
      .finally(() => { 
        this.loading = false;
        this.$emit("on-editing", "onNotEditing");
      })
    }
  },
  mounted() {
    storeUndoCommand.initUndoHistory( this.$store,[this.note]);
  }
};
</script>

<style scoped>
.note-body {
  padding-left: 10px;
  padding-right: 10px;
  border-radius: 10px;
  border-style: solid;
  border-top-width: 3px;
  border-bottom-width: 1px;
  border-right-width: 3px;
  border-left-width: 1px;
}

.note-title {
  margin-top: 0px;
  padding-top: 10px;
  color: black;
}

.outdated-label {
  display: inline-block;
  height: 100%;
  vertical-align: middle;
  margin-left: 20px;
  padding-bottom: 10px;
  color: red;
}
</style>
