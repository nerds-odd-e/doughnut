<template>
  <ContainerPage
    v-bind="{ contentExists: !!circle, title: `Circle: ${circle?.name}` }"
  >
    <div v-if="circle">
      <p>
        <NotebookNewButton :circle="circle">
          Add New Notebook In This Circle
        </NotebookNewButton>
      </p>

      <main>
        <NotebookCardsWithButtons :notebooks="circle.notebooks.notebooks">
          <template #default="{ notebook }">
            <NotebookButtons
              v-bind="{ notebook, user }"
              class="card-header-btn"
            >
              <template #additional-buttons>
                <BazaarNotebookButtons :notebook="notebook" :logged-in="true" />
              </template>
            </NotebookButtons>
          </template>
        </NotebookCardsWithButtons>
      </main>

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
import BazaarNotebookButtons from "@/components/bazaar/BazaarNotebookButtons.vue"
import NotebookButtons from "@/components/notebook/NotebookButtons.vue"
import NotebookCardsWithButtons from "@/components/notebook/NotebookCardsWithButtons.vue"
import NotebookNewButton from "@/components/notebook/NotebookNewButton.vue"
import SvgMissingAvatar from "@/components/svgs/SvgMissingAvatar.vue"
import { CircleForUserView, User } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { StorageAccessor } from "@/store/createNoteStorage"
import { PropType, defineComponent } from "vue"
import ContainerPage from "./commons/ContainerPage.vue"

export default defineComponent({
  setup() {
    return useLoadingApi()
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
    user: { type: Object as PropType<User> },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },

  data() {
    return {
      circle: null as CircleForUserView | null,
      timer: null as NodeJS.Timeout | null,
    }
  },

  methods: {
    async fetchData() {
      this.timer = setTimeout(() => {
        this.fetchData()
      }, 5000)
      this.circle = await this.managedApi.restCircleController.showCircle(
        this.circleId
      )
    },
  },

  computed: {
    invitationUrl() {
      return `${window.location.origin}/circles/join/${
        /* eslint-disable  @typescript-eslint/no-non-null-assertion */
        this.circle!.invitationCode
      }`
    },
  },

  mounted() {
    this.fetchData()
  },
  beforeUnmount() {
    if (this.timer) {
      clearTimeout(this.timer)
    }
  },
})
</script>

<style lang="sass" scoped>
#invitation-code
  width: 100%
</style>
