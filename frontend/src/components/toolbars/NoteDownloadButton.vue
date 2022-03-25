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
        mdString += `![image](${image})` + "\n"
    }
    if(url){
        mdString += `[url](${url})`
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
        const mdString = generateMD({...this.note.textContent, ...this.note.noteAccessories, image: this.note.pictureWithMask?.notePicture})
        const blob = new Blob([mdString], {type: "text/plain;charset=utf-8"});
        await saveAs(blob, `${this.note.title}.md`);

    }
  },
};
</script>
