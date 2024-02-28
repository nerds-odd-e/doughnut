<template>
  <ContainerPage
    v-bind="{
      contentExists: !!notebooksViewedByUser,
      title: 'Welcome To The Bazaar',
    }"
  >
    <p>These are shared notes from doughnut users.</p>
    <div v-if="notebooksViewedByUser">
      <NotebookBazaarViewCards
        :notebooks="notebooksViewedByUser.notebooks"
        :logged-in="!!user"
      />
    </div>
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { NotebooksViewedByUser, User } from "@/generated/backend";
import NotebookBazaarViewCards from "../components/bazaar/NotebookBazaarViewCards.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import useLoadingApi from "../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  components: { ContainerPage, NotebookBazaarViewCards },
  props: {
    user: {
      type: Object as PropType<User>,
      required: false,
    },
  },
  data() {
    return {
      notebooksViewedByUser: null as NotebooksViewedByUser | null,
    };
  },

  methods: {
    fetchData() {
      this.managedApi.restBazaarController.bazaar().then((res) => {
        this.notebooksViewedByUser = res;
      });
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
