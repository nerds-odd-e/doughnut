<template>
  <div>
    <TextInput scopeName='searchTerm' field='searchKey' v-model="searchTerm.searchKey" placeholder="Search"/>
    <CheckInput scopeName='searchTerm' field='searchGlobally' v-model="searchTerm.searchGlobally"/>
  </div>

  <div v-if="searchResult.length === 0">
      <em>No linkable notes found.</em>
  </div>
  <Cards v-else class="search-result" :notes="searchResult" columns="3">
    <template #button="{note}">
        <button class="btn btn-primary" v-on:click="$emit('selected', note)">Select</button>
    </template>
  </Cards>
</template>

<script>
import TextInput from "../form/TextInput.vue"
import CheckInput from "../form/CheckInput.vue"
import Cards from "../notes/Cards.vue"
import { restPost } from "../../restful/restful"

export default {
  name: 'SearchNote',
  props: { noteId: String },
  components: {TextInput, CheckInput, Cards},
  emits: ['selected'],
  data() {
    return {
      searchTerm: {
        searchKey: '',
        searchGlobally: false
      },
      searchResult: [],
    }
  },
  watch: {
    searchTerm: {
      handler(newSearchTerm) {
        if (newSearchTerm.searchKey.trim() === '') {
          this.searchResult = []
        }
        else {
          this.search()
        }

      },
      deep: true
    }
  },
  methods: {
    search() {
      restPost(`/api/notes/${this.noteId}/search`, {...this.searchTerm, searchKey: this.searchTerm.searchKey.trim()}, (r)=>{})
        .then(r=>this.searchResult=r)
    },
  }
}

</script>

<style scoped>
.search-result {
  max-height: 300px;
  overflow-y: auto;
}
</style>