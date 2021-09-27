<template>
  <h2>Notebooks</h2>
  <LoadingPage v-bind="{ loading, contentExists: !!notebooksViewedByUser }">
    <div v-if="!!notebooksViewedByUser">
      <p>
        <NotebookNewButton>Add New Notebook</NotebookNewButton>
      </p>
      <NotebookViewCards :notebooks="notebooksViewedByUser.notebooks" />
      <h2>Subscribed Notes</h2>
      <NotebookSubscriptionCards
        :subscriptions="notebooksViewedByUser.subscriptions"
        @updated="fetchData()"
      />
    </div>
  </LoadingPage>
</template>

<script>
import NotebookViewCards from "../components/notebook/NotebookViewCards.vue";
import NotebookNewButton from "../components/notebook/NotebookNewButton.vue";
import NotebookSubscriptionCards from "../components/subscriptions/NotebookSubscriptionCards.vue";
import LoadingPage from "./commons/LoadingPage.vue";
import { restGet } from "../restful/restful";

export default {
  name: "NotebooksPage",
  components: {
    LoadingPage,
    NotebookViewCards,
    NotebookSubscriptionCards,
    NotebookNewButton,
  },
  data() {
    return {
      loading: false,
      notebooksViewedByUser: null,
    };
  },
  methods: {
    fetchData() {
      this.loading = true
      restGet(`/api/notebooks`).then(
        (res) => (this.notebooksViewedByUser = res)
      ).finally(()=> this.loading = false);
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
