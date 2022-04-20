<template>
  <TextInput field="comment" v-focus/>
  <button>Submit</button>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import TextInput from "../form/TextInput.vue";

export default defineComponent({
  components: {
    TextInput,
  },
  emits: ["done"],
});
</script>
