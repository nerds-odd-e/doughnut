<template>
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
    <div class="col-12" v-if="!!noteContent.url">
      <label v-if="noteContent.urlIsVideo">Video Url:</label>
      <label v-else>Url:</label>
      <a :href="noteContent.url">{{ noteContent.url }}</a>
    </div>
  </div>
</template>

<script>
import ShowPicture from "./ShowPicture.vue";
import ShowDescription from "./ShowDescription.vue";

export default {
  props: {
    noteContent: { type: Object, required: true },
    notePicture: String,
  },
  components: {
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