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

<script setup lang="ts">
import BazaarNotebookButtons from "@/components/bazaar/BazaarNotebookButtons.vue"
import NotebookButtons from "@/components/notebook/NotebookButtons.vue"
import NotebookCardsWithButtons from "@/components/notebook/NotebookCardsWithButtons.vue"
import NotebookNewButton from "@/components/notebook/NotebookNewButton.vue"
import SvgMissingAvatar from "@/components/svgs/SvgMissingAvatar.vue"
import { CircleForUserView, User } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { StorageAccessor } from "@/store/createNoteStorage"
import { PropType, computed, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import ContainerPage from "./commons/ContainerPage.vue"

const { managedApi } = useLoadingApi()

const router = useRouter()

const { circleId } = defineProps({
  circleId: { type: Number, required: true },
  user: { type: Object as PropType<User> },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const circle = ref<CircleForUserView | null>(null)

const fetchData = async () => {
  circle.value = await managedApi.restCircleController.showCircle(circleId)
}

const invitationUrl = computed(() => {
  return `${window.location.origin}${
    router.resolve({
      name: "circleJoin",
      params: { invitationCode: circle.value?.invitationCode },
    }).href
  }`
})

onMounted(() => {
  fetchData()
})
</script>

<style lang="sass" scoped>
#invitation-code
  width: 100%
</style>
