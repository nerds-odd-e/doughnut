<template>
  <div class="note-show" :style="`background-color: ${bgColor}`">
    <NoteFrameOfLinks v-bind="{ links }" @updated="$emit('updated')">
      <div class="note-body" @dblclick="editDialog">
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
  </div>
</template>

<script>
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import NoteEditDialog from "./NoteEditDialog.vue";
import ShowPicture from "./ShowPicture.vue";
import ShowDescription from "./ShowDescription.vue";

export default {
  props: {
    id: Number,
    links: Object,
  },
  emits: ["updated"],
  components: {NoteFrameOfLinks, NoteEditDialog, ShowPicture, ShowDescription},
  computed: {
    note() {
      return this.$store.getters.getNoteById(this.id);
    },
    twoColumns() {return !!this.note.notePicture && !!this.note.noteContent.description },
    bgColor() {
      const colorOld = [150, 150, 150]
      const newColor = [208, 237, 23]
      const ageInMillisecond = Math.max(0, Date.now() - new Date(this.note.noteContent.updatedAt))
      const max = 15 // equals to 225 hours
      const index = Math.min(max, Math.sqrt(ageInMillisecond / 1000 / 60 / 60))
      return `rgb(${colorOld.map((oc, i)=>Math.round((oc * index + newColor[i] * (max-index))/max)).join(',')})`
    }
  },
  methods: {
    async editDialog() {
      if (
        await this.$popups.dialog(NoteEditDialog, {
          noteId: this.id,
        })
      ) {
        this.$emit("updated");
      }
    }
  },
}

</script>

<style scoped>
.note-show {
  border-radius: 10px;
  border-top: solid 1px black;
  border-bottom: solid 1px black;
}

.link-multi + .link-multi::before {
  padding-right: 0.5rem;
  color: #6c757d;
  content: "|";
}

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
