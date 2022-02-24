<template>
  <div>
    <EditableText
        :multipleLine="false"
        role="comment-form"
        class="note-title"
        scopeName="note"
        :showInputBox="true"
        v-model="textContent.description"
        @blur="onBlurTextField"/>
  </div>
</template>

<script>
import EditableText from "../form/EditableText.vue";
import { storedApi } from "../../storedApi";


export default {
  props:{
    noteId: String,
    comment: Object,
  },
  components:{
    EditableText
  },
  
  data() {
    return {
      comment:{textContent: 'Add a comment'} ,
      loading: false,
      formErrors: {},
    };
  },
  computed: {
    textContent(){
      return {...this.comment.textContent};
    },
  },
  methods: {
    onBlurTextField(){
      this.loading = true
      storedApi(this.$store).addCommentToNote(this.noteId, this.textContent)
      .then((res) => {
        this.$emit("done");
      })
      .catch((res) => (this.formErrors = res))
      .finally(() => { 
        this.loading = false;
      })
    }
  }

}

</script>
