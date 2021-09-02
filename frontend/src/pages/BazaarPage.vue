<template>
  <h2>Welcome To The Bazaar</h2>
  <p>These are shared notes from doughnut users.</p>
  <LoadingPage v-bind="{ loading, contentExists: !!notebooksViewedByUser }">
    <div v-if="!!notebooksViewedByUser">
      <NotebookBazaarViewCards
        :notebooks="notebooksViewedByUser.notebooks"
        :user="user"
      />
    </div>
  </LoadingPage>
</template>

<script>
import NotebookBazaarViewCards from "../components/bazaar/NotebookBazaarViewCards.vue";
import LoadingPage from "./commons/LoadingPage.vue";
import { restGet } from "../restful/restful";

export default {
  name: "NotebooksPage",
  props: { user: Object },
  components: { LoadingPage, NotebookBazaarViewCards },
  data() {
    return {
      loading: false,
      notebooksViewedByUser: null,
    };
  },
  methods: {
    fetchData() {
      restGet(`/api/bazaar`, (r) => (this.loading = r)).then(
        (res) => (this.notebooksViewedByUser = res)
      );
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
