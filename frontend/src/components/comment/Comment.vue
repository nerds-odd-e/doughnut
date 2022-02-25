<template>
  <div class="card">
  <span>{{ comment.textContent.title }}</span>
  <button
    v-if="shouldShowDelete"
    class="btn btn-sm"
    title="delete comment"
    v-on:click="deleteComment"
  >
    <SvgRemove />
    Delete
  </button>
  </div>
</template>

<script>
import SvgRemove from "../svgs/SvgRemove.vue";

export default {
  components: {
    SvgRemove
  },
  props: {
    comment: { type: Object, required: true }
  },
  computed: {
    user() { return this.$store.getters.getCurrentUser()},
    shouldShowDelete() {
      return this.user.id === this.comment.user.id || true
    }
  },
  methods: {
    deleteComment(){
    }
  }
}
</script>