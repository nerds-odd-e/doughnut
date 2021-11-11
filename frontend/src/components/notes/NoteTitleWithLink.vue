<template>
  <router-link
    :to="{ name: 'noteShow', params: { noteId: note.id, viewType: computedViewType } }"
    class="text-decoration-none"
  >
    {{ translatedTitle }}
  </router-link>
</template>

<script>
import Languages from "../../constants/lang";

export default {
  props: {
    note: { type: Object, required: true }, 
    viewType: String,
  },
  computed: {
    computedViewType() {
      if(this.viewType) return this.viewType
      return this.$route?.params?.viewType
    },
    translatedTitle(){
      if (!this.note.noteContent)
        return this.note.title;

      return this.note.language === Languages.ID && this.note.noteContent.titleIDN ? this.note.noteContent.titleIDN : this.note.noteContent.title;
    }
  }

}

</script>
