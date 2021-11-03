<template>
  <template v-if="!!note">
    <router-link
      :to="{ name: 'noteShow', params: { noteId: note.id, viewType} }"
      class="text-decoration-none"
    >
      {{ note.title }}
    </router-link>
  </template>
</template>

<script>
export default {
  props: {
    note: Object, 
    noteRouteName: { type: String, default: 'noteCards'},
  },
  computed: {
    viewType() {
      if(this.noteRouteName === 'noteCards') return 'cards'
      if(this.noteRouteName === 'noteMindmap') return 'mindmap'
      if(this.noteRouteName === 'noteArticle') return 'article'
      return this.$route.params?.viewType
    }
  }

}

</script>
