<template>
  <a class="btn btn-sm" id="show-comment">Show Comment</a>
  <template :key="index" v-for="(comment,index) in comments">
    <Comment :content="comment.content"></Comment>
  </template>
</template>

<script>
import Comment from "../comment/Comment.vue"
  export default {
    name: "NoteShowCommentButton",
    props: {
      comments: Array
    },
    components: {
      Comment
    }
  }
</script>