<template>
  <slot v-if="note" :note="note" />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi({ initalLoading: true, hasFormError: false });
  },
  props: { noteId: { type: Number, required: true } },
  emits: ["done"],
  data() {
    return {
      note: undefined as undefined | Generated.Note,
    };
  },
  methods: {
    fetchData() {
      this.api.noteMethods.getNoteRealmWithPosition(this.noteId).then((res) => {
        this.note = res.noteRealm.note;
      });
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
