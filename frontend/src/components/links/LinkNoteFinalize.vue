<template>
  <div> Target: <strong>{{targetNote.title}}</strong> </div>

  <div>
      <LinkTypeSelect scopeName='link' v-model="formData.typeId" :errors="formErrors.pictureMask"/>
      <button class="btn btn-secondary go-back-button" v-on:click="$emit('goBack')"><SvgGoBack/></button>
      <button class="btn btn-primary" v-on:click="createLink()">Create Link</button>
  </div>
</template>

<script>
import LinkTypeSelect from "./LinkTypeSelect.vue"
import SvgGoBack from "../svgs/SvgGoBack.vue"
import { restPost } from "../../restful/restful"

export default {
  name: 'LinkNoteFinalize',
  props: { noteId: Number, targetNote: {type: Object, required: true} },
  components: {LinkTypeSelect, SvgGoBack},
  emits: [ 'success', 'goBack' ],
  data() {
    return {
      formData: {},
      formErrors: {},
    }
  },
  methods: {
    createLink() {
      restPost(`/api/links/create/${this.noteId}/${this.targetNote.id}`, this.formData,
        (r)=>{},
        (r)=>this.$emit('success'),
        (res) => this.formErrors = res,
      )
    }
  }

}

</script>
