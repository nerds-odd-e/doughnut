<template>
  <h3>
    Associate <strong>{{ note.title }}</strong> to WIKI
  </h3>
  <form @submit.prevent.once="saveWiki">
    <TextInput
      scopeName="wikiID"
      field="wikiID"
      v-model="associationData.wikiDataId"
      placeholder="Q1234"
    />
    <input type="submit" value="Save" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import TextInput from "../form/TextInput.vue";

export default defineComponent({
  props: { note: Object as PropType<Generated.Note> },
  components: { TextInput },
  emits: ["done"],
  data() {
    return {
      associationData: {
        noteId: 0,
        wikiDataId: "",
      } as Generated.WikiAssociation,
    };
  },
  computed: {
    payload() {
      return {
        noteId: this.note?.id,
        wikiDataId: this.associationData.wikiDataId,
      };
    },
  },
  methods: {
    async saveWiki() {
      console.log("payload", this.payload);
      this.$emit("done");
    },
  },
});
</script>
