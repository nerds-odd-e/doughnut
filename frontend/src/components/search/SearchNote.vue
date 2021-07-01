<template>

<form :action="`/links/${noteId}/search_for_target`" method="post">
    <TextInput scopeName='searchTerm' field='searchKey' v-model="searchTerm.searchKey" placeholder="Search"/>
    <CheckInput scopeName='searchTerm' field='searchGlobally' v-model="searchTerm.searchGlobally"/>
    <button type="submit" class="btn btn-primary">Search</button>
</form>

</template>

<script>
import TextInput from "../form/TextInput.vue"
import CheckInput from "../form/CheckInput.vue"
export default {
  name: 'SearchNote',
  props: { noteId: String },
  components: {TextInput, CheckInput},
  data() {
    return {
      searchTerm: {
        searchKey: null,
        searchGlobally: false
      }
    }
  }
}

</script>
