<template>
  <h3>
    Associate <strong>{{ note.title }}</strong> to WIKI
  </h3>
  <form @submit.prevent.once="saveWiki">
    <TextInput
      scopeName="wikiID"
      field="wikiID"
      v-model="associationData.wikidataId"
      placeholder="Q1234"
    />
    <input type="submit" value="Save" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import TextInput from "../form/TextInput.vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";

export default defineComponent({
  setup() {
    return useStoredLoadingApi({ initalLoading: true, hasFormError: true });
  },
  props: { note: {type: Object as PropType<Generated.Note>, required: true } },
  components: { TextInput },
  emits: ["done"],
  data() {
    return {
      noteId: 0,
      associationData: {
        wikidataId: ""
      } as Generated.WikidataAssociationCreation,
    };
  },
  computed: {
    payload() {
      return {
        noteId: this.note.id,
        associationData: {
          wikidataId: this.associationData.wikidataId,
        }
      };
    },
  },
  methods: {
    saveWiki() {
      this.storedApi
      .updateWikidataId(this.payload.noteId, this.payload.associationData)
      .then(() => {
        this.$emit("done");
      })
    },
  },
});
</script>
