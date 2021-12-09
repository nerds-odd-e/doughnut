<template>
  <button class="btn btn-small" title="Download as MD" id="note-download-button"  @click="saveAsMD()" >
    <SvgDownload />
  </button>
</template>

<script>
import SvgDownload from "../svgs/SvgDownload.vue";
import { saveAs } from 'file-saver';

const generateMD = ({title, description, image, url}) => {
    let mdString = ""
    if(title){
      mdString += "# " + title  + "\n"
    }
    if(description){
      mdString += description + "\n"
    }
    if(image){
        mdString += `!(image)[${window.location.origin + image}]` + "\n"
    }
    if(url){
        mdString += `(url)[${url}]` + "\n"
    }
    return mdString
}

export default {
  name: "DownloadButton",
  components: {
    SvgDownload,
  },
  props: { note:Object },
  methods: {
    async saveAsMD(){
        console.log(this.note)
        const title = this.note.title
        const mdString = generateMD({...this.note.noteContent, image: this.note.notePicture})
        const blob = new Blob([mdString], {type: "text/plain;charset=utf-8"});
        await saveAs(blob, `${title}.md`);

    }
  },
};
</script>
