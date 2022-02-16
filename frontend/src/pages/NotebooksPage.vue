<template>
  <NoteControl/>
  <ContainerPage v-bind="{ loading, contentExists: true, title: 'Notebooks' }">
    <p>
      <NotebookNewButton>Add New Notebook</NotebookNewButton>
    </p>
    <NotebookViewCards :notebooks="notebooks" />
    <h2>Subscribed Notes</h2>
    <NotebookSubscriptionCards
      :subscriptions="subscriptions"
      @updated="fetchData()"
    />
  </ContainerPage>
</template>

<script>
import NotebookViewCards from "../components/notebook/NotebookViewCards.vue";
import NotebookNewButton from "../components/notebook/NotebookNewButton.vue";
import NoteControl from "../components/toolbars/NoteControl.vue";
import NotebookSubscriptionCards from "../components/subscriptions/NotebookSubscriptionCards.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import { storedApiGetNotebooks, storedApiUndoDeleteNote } from "../storedApi";
import { useSnackbarPlugin } from "snackbar-vue";

export default {
  name: "NotebooksPage",
  components: {
    ContainerPage,
    NoteControl,
    NotebookViewCards,
    NotebookSubscriptionCards,
    NotebookNewButton,
  },
  data() {
    return {
      loading: false,
      subscriptions: null,
    };
  },
  computed: {
    notebooks() {
      return this.$store.getters.getNotebooks()
    }
  },
  methods: {
    fetchData() {
      this.loading = true
      storedApiGetNotebooks(this.$store).then(
        (res) => (this.subscriptions = res.subscriptions)
      ).finally(()=> this.loading = false);
    },
    showUndoSnackbar() {
      const snack = useSnackbarPlugin();
      const deletedNoteId = this.$store.getters.getLastDeletedNoteId()
      if(!deletedNoteId) return
      snack.show({
        position: 'bottom',
        text: `Note successfully deleted`,
        button: 'Undo',
        action: () => {
          storedApiUndoDeleteNote(this.$store);
        },
        time: 5000
      });
    }
  },
  mounted() {
    this.fetchData();
    this.showUndoSnackbar();
  },
};
</script>
