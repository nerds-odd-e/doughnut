<template>
  <ContainerPage v-bind="{ contentExists: true, title: 'Notebooks' }">
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

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NotebookViewCards from "../components/notebook/NotebookViewCards.vue";
import NotebookNewButton from "../components/notebook/NotebookNewButton.vue";
import NotebookSubscriptionCards from "../components/subscriptions/NotebookSubscriptionCards.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import { StorageAccessor } from "../store/createNoteStorage";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    selectedNoteId: Number,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  name: "NotebooksPage",
  components: {
    ContainerPage,
    NotebookViewCards,
    NotebookSubscriptionCards,
    NotebookNewButton,
  },
  data() {
    return {
      subscriptions: undefined as Generated.Subscription[] | undefined,
      notebooks: undefined as Generated.NotebookViewedByUser[] | undefined,
    };
  },
  methods: {
    fetchData() {
      this.storageAccessor.selectPosition();
      this.api.notebookMethods.getNotebooks().then((res) => {
        this.notebooks = res.notebooks;
        this.subscriptions = res.subscriptions;
      });
    },
  },
  watch: {
    "storageAccessor.updatedAt": function updateAt() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
