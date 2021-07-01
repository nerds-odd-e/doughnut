<template>

<div v-if="!!targetNote">
        <div> To <strong>{{targetNote.title}}</strong> </div>

        <form :action="`/links/create_link`" method="post">
            <input name="sourceNote" :value="noteId" type="hidden"/>
            <input name="targetNote" :value="targetNote.id" type="hidden"/>
            <Select v-if="!!$staticInfo" scopeName='link' field='typeId' v-model="formData.typeId" :options="$staticInfo.linkTypeOptions"/>
            <input type="submit" value="Link" class="btn btn-primary"/>
        </form>
</div>
<div v-else>
  <div>
    <TextInput scopeName='searchTerm' field='searchKey' v-model="searchTerm.searchKey" placeholder="Search"/>
    <CheckInput scopeName='searchTerm' field='searchGlobally' v-model="searchTerm.searchGlobally"/>
  </div>

  <div v-if="searchResult.length === 0">
      <em>No linkable notes found.</em>
  </div>
  <Cards v-else :notes="searchResult">
    <template #button="{note}">
        <button class="btn btn-primary" v-on:click="targetNote=note">Select</button>
    </template>
  </Cards>
</div>

</template>

<script>
import TextInput from "../form/TextInput.vue"
import CheckInput from "../form/CheckInput.vue"
import Select from "../form/Select.vue"
import Cards from "../notes/Cards.vue"
import { restPost } from "../../restful/restful"

export default {
  name: 'SearchNote',
  props: { noteId: String },
  components: {TextInput, CheckInput, Cards, Select},
  data() {
    return {
      searchTerm: {
        searchKey: '',
        searchGlobally: false
      },
      searchResult: [],
      targetNote: null,
      formData: {},
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
      restPost(`/api/notes/${this.noteId}/search`, {...this.searchTerm, searchKey: this.searchTerm.searchKey.trim()}, (r)=>{}, (r)=>this.searchResult=r)
    }
  }

}

</script>
