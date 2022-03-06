<template>
  <ContainerPage v-bind="{ loading, contentExists: !!circle }">
    <div v-if="circle">
      <h1 v-text="circle.name" />
      <p>
        <NotebookNewButton :circle="circle">
          Add New Notebook In This Circle
        </NotebookNewButton>
      </p>

      <NotebookCardsWithButtons :notebooks="circle.notebooks">
        <template #default="{ notebook }">
          <NotebookButtons v-bind="{ notebook, featureToggle }" class="card-header-btn">
            <template #additional-buttons>
              <BazaarNotebookButtons :notebook="notebook" :user="true" />
            </template>
          </NotebookButtons>
        </template>
      </NotebookCardsWithButtons>

      <nav class="nav justify-content-end">
        <div
          class="nav-item circle-member"
          v-for="member in circle.members"
          :key="member.id"
        >
          <span :title="member.name"> <SvgMissingAvatar /> </span>
        </div>
      </nav>

      <h2>Invite People To Your Circle</h2>
      Please share this invitation code so that they can join your circle:

      <div class="jumbotron">
        <input id="invitation-code" :value="invitationUrl" readonly />
      </div>
    </div>
  </ContainerPage>
</template>

<script>
import SvgMissingAvatar from "../components/svgs/SvgMissingAvatar.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import NotebookCardsWithButtons from "../components/notebook/NotebookCardsWithButtons.vue";
import NotebookNewButton from "../components/notebook/NotebookNewButton.vue";
import NotebookButtons from "../components/notebook/NotebookButtons.vue";
import BazaarNotebookButtons from "../components/bazaar/BazaarNotebookButtons.vue";
import storedApi from  "../managedApi/storedApi";
import { useStore } from "@/store";

export default {
  setup() {
    const store = useStore()
    return { store }
  },
  components: {
    SvgMissingAvatar,
    NotebookCardsWithButtons,
    NotebookButtons,
    NotebookNewButton,
    BazaarNotebookButtons,
    ContainerPage,
  },
  props: { circleId: [String, Number] },

  data() {
    return {
      queryCounter: 0,
      circle: null,
      loading: true,
      formErrors: {},
    };
  },

  methods: {
    getCircle() {
      storedApi(this).getCircle(this.circleId)
      .then((res) =>  this.circle = res )
      this.queryCounter += 1
    },
    fetchData() {
      this.getCircle()
    },
  },

  computed: {
    invitationUrl() {
      return `${window.location.origin}/circles/join/${this.circle.invitationCode}`;
    },
    featureToggle() { return this.store.getFeatureToggle()}
  },

  watch: {
    queryCounter() {
      setTimeout(() => {
        this.getCircle()
      }, 5000);

    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>

<style lang="sass" scoped>
#invitation-code
  width: 100%
</style>

