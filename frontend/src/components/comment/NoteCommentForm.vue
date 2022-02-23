<template>
  <div>
    <EditableText
        :multipleLine="false"
        role="title"
        class="note-title"
        scopeName="note"
        :showInputBox="true"
        v-model="textContent.title"
        @blur="onBlurTextField"/>
  </div>
</template>

<script>
import EditableText from "../form/EditableText.vue";

export default {
  props:{
    comment: Object,
  },
  components:{
    EditableText
  },
  
  data() {
    return {
      comment:{textContent: 'Add a comment'} ,
      loading: false,
      formErrors: {},
    };
  },
  computed: {
    textContent(){
      return {...this.comment.textContent};
    },
  },
  methods: {
    onBlurTextField(){
      console.log('test')
    }
  }

}

</script>
