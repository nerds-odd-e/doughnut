<template>
    <NoteShell class="note-body" v-bind="{id, updatedAt: noteContent.updatedAt}">
      <NoteFrameOfLinks v-bind="{ links }">
        <h2 role="title" class="note-title">{{ title }}</h2>
        <div class="row">
          <ShowDescription
            :class="`col-12 ${twoColumns ? 'col-md-6' : ''}`"
            :description="noteContent.description"
          />
          <ShowPicture
            :class="`col-12 ${twoColumns ? 'col-md-6' : ''}`"
            v-bind="{notePicture, pictureMask: noteContent.pictureMask}"
            :opacity="0.2"
          />
        </div>
        <div v-if="!!noteContent.url">
          <label v-if="noteContent.urlIsVideo">Video Url:</label>
          <label v-else>Url:</label>
          <a :href="noteContent.url">{{ noteContent.url }}</a>
        </div>
      </NoteFrameOfLinks>
    </NoteShell>
</template>

<script>
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import NoteShell from "./NoteShell.vue";
import ShowPicture from "./ShowPicture.vue";
import ShowDescription from "./ShowDescription.vue";

export default {
  name: "NoteWithLinks",
  props: {
    id: [String, Number],
    noteContent: { type: Object, required: true },
    title: String,
    notePicture: String,
    links: Object,
  },
  components: {
    NoteFrameOfLinks,
    NoteShell,
    ShowPicture,
    ShowDescription,
  },
  computed: {
    twoColumns() {
      return !!this.notePicture && !!this.noteContent.description;
    },
  },
};
</script>

<style scoped>

.link-multi + .link-multi::before {
  padding-right: 0.5rem;
  color: #6c757d;
  content: "|";
}

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
</style>
