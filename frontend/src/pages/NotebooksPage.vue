<template>
  <ContainerPage v-bind="{ loading, contentExists: !!notebooksViewedByUser, title: 'Notebooks' }">
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
  </ContainerPage>
</template>

<script>
import NotebookViewCards from "../components/notebook/NotebookViewCards.vue";
import NotebookNewButton from "../components/notebook/NotebookNewButton.vue";
import NotebookSubscriptionCards from "../components/subscriptions/NotebookSubscriptionCards.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import { restGet, restPatch } from "../restful/restful";
import { useSnackbarPlugin } from "snackbar-vue";

export default {
  name: "NotebooksPage",
  components: {
    ContainerPage,
    NotebookViewCards,
    NotebookSubscriptionCards,
    NotebookNewButton,
  },
  computed: {
    featureToggle() {
      return this.$store.getters.getFeatureToggle();
    },
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
    handleUndoDelete(id) {
      restPatch(`/api/notes/${id}/undo-delete`).then(
        (res) => this.fetchData()
      );
    },
    showUndoSnackbar() {
      const snack = useSnackbarPlugin();
      const { deletedNoteId } = this.$route.query;
      if(deletedNoteId) {
        snack.show({
          position: 'bottom',
          text: `Note successfully deleted`,
          button: 'Undo',
          action: () => this.handleUndoDelete(deletedNoteId),
          time: 5000
        });
      }
    }
  },
  mounted() {
    this.fetchData();
    if(this.featureToggle) {
      this.showUndoSnackbar();
    }
  },
};
</script>
