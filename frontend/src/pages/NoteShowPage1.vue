<template>
  <ContainerPage v-bind="{ loading, contentExists: !!notePosition }">
  </ContainerPage>
</template>

<script>
import ContainerPage from "./commons/ContainerPage.vue";
import { storedApiGetNoteAndItsChildren } from "../storedApi";

export default {
  props: { noteId: [String, Number] },
  data() {
    return {
      notePosition: null,
      loading: true,
    };
  },
  components: { ContainerPage },
  methods: {
    fetchData() {
      this.loading = true
      storedApiGetNoteAndItsChildren(this.$store, this.noteId)
      .then((res) => {
        this.notePosition = res.notePosition;
      }).finally(() => this.loading = false);
    },
  },
  updated() {
    this.fetchData();
  },
  mounted() {
    this.fetchData();
  },
};

</script>
