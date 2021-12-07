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
  props: { note:Object },
  methods: {
    async saveAsMD(){
        const title = this.note.title
        const description =  this.note.description
        const blob = new Blob([description], {type: "text/plain;charset=utf-8"});
        await saveAs(blob, `${title}.md`);

    }
  },
};
</script>
