<template>
  <ContainerPage
    v-bind="{ contentLoaded: circle !== undefined, title: `Circle: ${circle?.name}` }"
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
            />
          </template>
        </NotebookCardsWithButtons>
      </main>

      <nav class="daisy-flex daisy-justify-end">
        <div
          class="daisy-flex-none circle-member"
          v-for="member in circle.members"
          :key="member.name"
        >
          <span :title="member.name"> <SvgMissingAvatar /> </span>
        </div>
      </nav>

      <h2>Invite People To Your Circle</h2>
      Please share this invitation code so that they can join your circle:

      <div class="daisy-hero daisy-bg-base-200">
        <input id="invitation-code" :value="invitationUrl" readonly class="daisy-input daisy-input-bordered daisy-w-full" />
      </div>
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import NotebookButtons from "@/components/notebook/NotebookButtons.vue"
import NotebookCardsWithButtons from "@/components/notebook/NotebookCardsWithButtons.vue"
import NotebookNewButton from "@/components/notebook/NotebookNewButton.vue"
import SvgMissingAvatar from "@/components/svgs/SvgMissingAvatar.vue"
import type { CircleForUserView, User } from "@generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType, Ref } from "vue"
import { computed, inject, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import ContainerPage from "./commons/ContainerPage.vue"

const { managedApi } = useLoadingApi()

const router = useRouter()

const { circleId } = defineProps({
  circleId: { type: Number, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const user = inject<Ref<User | undefined>>("currentUser")
const circle = ref<CircleForUserView | undefined>(undefined)

const fetchData = async () => {
  circle.value = await managedApi.services.showCircle({ circle: circleId })
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
