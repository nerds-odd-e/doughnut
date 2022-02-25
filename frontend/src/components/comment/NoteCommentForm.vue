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
  <div>
    <span>{{commentx}}</span>
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
      commentx: null,
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
    fetchComments() {
      this.commentx = 'please elaborate'

    },
    onBlurTextField(){
      this.loading = true
      storedApi(this.$store).addCommentToNote(this.noteId, this.textContent)
      .then((res) => {
        this.fetchComments();
      })
      .catch((res) => (this.formErrors = res))
      .finally(() => { 
        this.loading = false;
      })
    }
  }

}

</script>
