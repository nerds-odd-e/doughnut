import LoadingModal from "@/components/commons/LoadingModal.vue"
import {
  currentBlockingApiState,
  type ApiStatus,
} from "@/managedApi/ApiStatusHandler"
import { setupGlobalClient } from "@/managedApi/clientSetup"
import { computed, defineComponent, ref } from "vue"

export default defineComponent({
  name: "GlobalApiLoadingModal",
  components: { LoadingModal },
  setup() {
    const apiStatus = ref<ApiStatus>({ states: [] })
    setupGlobalClient(apiStatus.value)
    const blockingApiState = computed(() =>
      currentBlockingApiState(apiStatus.value)
    )
    return { blockingApiState }
  },
  template: `
    <LoadingModal
      :show="!!blockingApiState"
      :message="blockingApiState?.message"
      :cancel-control="
        blockingApiState?.cancel
          ? { id: blockingApiState.id, action: blockingApiState.cancel }
          : undefined
      "
    />
  `,
})
