<template>
  <div class="daisy-join">
    <PopButton title="Edit subscription">
      <template #button_face>
        <Pencil class="w-5 h-5" />
      </template>
      <SubscriptionEditDialog
        :subscription="subscription"
        @done="doneHandler()"
      />
    </PopButton>
    <button
      class="daisy-btn daisy-btn-ghost daisy-btn-sm"
      title="Unsubscribe"
      @click="processForm()"
    >
      <Minus class="w-5 h-5" />
    </button>
  </div>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue"
import type { Subscription } from "@generated/doughnut-backend-api"
import { SubscriptionController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import PopButton from "../commons/Popups/PopButton.vue"
import usePopups from "../commons/Popups/usePopups"
import { Minus, Pencil } from "lucide-vue-next"
import SubscriptionEditDialog from "./SubscriptionEditDialog.vue"

export default defineComponent({
  setup() {
    return usePopups()
  },
  props: {
    subscription: {
      type: Object as PropType<Subscription>,
      required: true,
    },
  },
  emits: ["updated"],
  components: { Minus, Pencil, PopButton, SubscriptionEditDialog },
  methods: {
    async processForm() {
      if (
        await this.popups.confirm(`Confirm to unsubscribe from this notebook?`)
      ) {
        const { error } = await apiCallWithLoading(() =>
          SubscriptionController.destroySubscription({
            path: { subscription: this.subscription.id },
          })
        )
        if (!error) {
          this.$emit("updated")
        }
      }
    },
    doneHandler() {
      this.$emit("updated")
    },
  },
})
</script>
