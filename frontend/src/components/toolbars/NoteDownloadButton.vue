<template>
  <button class="btn btn-small" title="Download as MD" id="note-download-button"  @click="saveAsMD()" >
    <SvgDownload />
  </button>
</template>

<script>
import SvgDownload from "../svgs/SvgDownload.vue";
import { saveAs } from 'file-saver';

export default {
  name: "DownloadButton",
  components: {
    SvgDownload,
  },
  props: { noteId: [String, Number], language: String },
  methods: {
    async saveAsMD(){
        const blob = new Blob(["Hello, world!"], {type: "text/plain;charset=utf-8"});
        await saveAs(blob, "Singapore.md");

    }
  },
};
</script>
