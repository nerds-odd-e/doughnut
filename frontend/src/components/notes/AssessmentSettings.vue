<template>
  <div>
    <TextInput rows="1" field="numberOfQuestion" />
    <button :disabled="numberOfQuestion < 0">Save Settings</button>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";

export default defineComponent({
  setup() {
    // return useLoadingApi();
  },
  props: {
    note: {
      type: Object as PropType<Note>,
      required: true,
    },
  },
  data() {
    return {
      numberOfQuestion: 0,
    };
  },
  methods: {
  },
  mounted() {
  },
});
</script>

<style scoped>

</style>
