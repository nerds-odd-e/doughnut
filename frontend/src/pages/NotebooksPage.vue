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
import NotebookViewCards from "@/components/notebook/NotebookViewCards.vue";
import NotebookNewButton from "@/components/notebook/NotebookNewButton.vue";
import NoteControl from "@/components/toolbars/NoteControl.vue";
import NotebookSubscriptionCards from "@/components/subscriptions/NotebookSubscriptionCards.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import storedApi from  "@/managedApi/storedApi";
import { useStore } from "@/store";

export default {
  setup() {
    const store = useStore()
    return { store }
  },
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
      return this.store.getNotebooks()
    },
  },
  methods: {
    fetchData() {
      storedApi(this).getNotebooks().then(
        (res) => (this.subscriptions = res.subscriptions))
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
