<template>
  <router-link
    :to="{ name: 'noteShow', params: { noteId: note.id, viewType: computedViewType } }"
    class="text-decoration-none"
  >
    {{ note.title }}
  </router-link>
</template>

<script>
export default {
  props: {
    note: { type: Object, required: true }, 
    viewType: String,
  },
  computed: {
    computedViewType() {
      if(this.viewType) return this.viewType
      return this.$route?.params?.viewType
    }
  }

}

</script>
