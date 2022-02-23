
<template>
  <a class="btn btn-sm" @click="toggleComment" id="show-comment">Show Comment</a>
  <Comment v-if="showComment" content="comment1"/>
</template>

<script>
import Comment from "../comment/Comment.vue"
  export default {
    name: "NoteShowCommentButton",
    data() {
      return {
        showComment: false 
      }
    },
    components: {
      Comment
    },
    methods: {
      toggleComment() {
        this.showComment = !this.showComment
      }
    }
  }
</script>