<template>

<div v-if="!!targetNote">
        <div> To <strong>{{targetNote.title}}</strong> </div>

        <div>
            <Select v-if="!!$staticInfo" scopeName='link' field='typeId' v-model="formData.typeId" :options="$staticInfo.linkTypeOptions" :errors="formErrors.pictureMask"/>
            <button class="btn btn-primary" v-on:link="createLink()">Link</button>
        </div>
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
  name: 'LinkNote',
  props: { noteId: String },
  components: {TextInput, CheckInput, Cards, Select},
  emits: [ 'done' ],
  data() {
    return {
      searchTerm: {
        searchKey: '',
        searchGlobally: false
      },
      searchResult: [],
      targetNote: null,
      formData: {},
      formErrors: {},
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
    },
    createLink() {
      restPost(`/api/notes/${this.noteId}/link`, this.formData, (r)=>{},
        (r)=>this.$emit('done', true),
        (res) => this.formErrors = res,
      )
    }
  }

}

</script>
