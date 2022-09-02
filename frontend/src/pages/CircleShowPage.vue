<template>
  <ContainerPage v-bind="{ loading, contentExists: !!circle }">
    <div v-if="circle">
      <p>
        <NotebookNewButton :circle="circle">
          Add New Notebook In This Circle
        </NotebookNewButton>
      </p>

      <NotebookCardsWithButtons :notebooks="circle.notebooks.notebooks">
        <template #default="{ notebook }">
          <NotebookButtons v-bind="{ notebook }" class="card-header-btn">
            <template #additional-buttons>
              <BazaarNotebookButtons :notebook="notebook" :logged-in="true" />
            </template>
          </NotebookButtons>
        </template>
      </NotebookCardsWithButtons>

      <nav class="nav justify-content-end">
        <div
          class="nav-item circle-member"
          v-for="member in circle.members"
          :key="member.name"
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

<script lang="ts">
import { defineComponent, PropType } from "vue";
import SvgMissingAvatar from "../components/svgs/SvgMissingAvatar.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import NotebookCardsWithButtons from "../components/notebook/NotebookCardsWithButtons.vue";
import NotebookNewButton from "../components/notebook/NotebookNewButton.vue";
import NotebookButtons from "../components/notebook/NotebookButtons.vue";
import BazaarNotebookButtons from "../components/bazaar/BazaarNotebookButtons.vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import { StorageAccessor } from "../store/createNoteStorage";

export default defineComponent({
  setup() {
    return useLoadingApi({ initalLoading: true, hasFormError: true });
  },
  components: {
    SvgMissingAvatar,
    NotebookCardsWithButtons,
    NotebookButtons,
    NotebookNewButton,
    BazaarNotebookButtons,
    ContainerPage,
  },
  props: {
    circleId: { type: Number, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },

  data() {
    return {
      circle: null as Generated.CircleForUserView | null,
      timer: null as NodeJS.Timeout | null,
    };
  },

  methods: {
    fetchData() {
      this.timer = setTimeout(() => {
        this.fetchData();
      }, 5000);
      this.api.circleMethods.getCircle(this.circleId).then((res) => {
        this.circle = res;
        this.storageAccessor.selectPosition(undefined, undefined, res);
      });
    },
  },

  computed: {
    invitationUrl() {
      return `${window.location.origin}/circles/join/${
        /* eslint-disable  @typescript-eslint/no-non-null-assertion */
        this.circle!.invitationCode
      }`;
    },
  },

  mounted() {
    this.fetchData();
  },
  beforeUnmount() {
    if (this.timer) {
      clearTimeout(this.timer);
    }
  },
});
</script>

<style lang="sass" scoped>
#invitation-code
  width: 100%
</style>
