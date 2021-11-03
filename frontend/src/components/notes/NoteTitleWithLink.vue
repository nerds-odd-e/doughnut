<template>
  <template v-if="!!note">
    <router-link
      :to="{ name: 'noteShow', params: { noteId: note.id, viewType: $route.params.viewType} }"
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
      if(noteRouteName === 'noteCards') return 'cards'
      if(noteRouteName === 'noteMindmap') return 'mindmap'
      if(noteRouteName === 'noteArticle') return 'article'
      return this.$route.params.viewType
    }
  }

}

</script>
