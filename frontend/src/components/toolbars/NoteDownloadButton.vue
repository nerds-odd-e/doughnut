<template>
  <button class="btn btn-small" title="Download as MD" id="note-download-button" >
    <SvgDownload />
  </button>
</template>

<script>
import SvgDownload from "../svgs/SvgDownload.vue";

export default {
  name: "DownloadButton",
  components: {
    SvgDownload,
  },
  props: { noteId: [String, Number], language: String },
  methods: {
    async saveAsMD(){
      
    }
  },
};
</script>
