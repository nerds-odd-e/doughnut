<template>
  <div> Target: <strong>{{targetNote.title}}</strong> </div>

  <div>
      <Select v-if="!!$staticInfo" scopeName='link' field='typeId' v-model="formData.typeId" :options="$staticInfo.linkTypeOptions" :errors="formErrors.pictureMask"/>
      <button class="btn btn-primary" v-on:click="createLink()">Create Link</button>
  </div>
</template>

<script>
import Select from "../form/Select.vue"
import { restPost } from "../../restful/restful"

export default {
  name: 'LinkNoteFinalize',
  props: { noteId: String, targetNote: {type: Object, required: true} },
  components: {Select},
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
