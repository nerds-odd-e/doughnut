
<template>
  <a class="btn btn-sm" id="show-comment">Show Comment</a>
  <Comment :content="comments[0].content"/>
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