<template>
  <div class="daisy-join">
    <PopButton title="Edit subscription">
      <template #button_face>
        <SvgEdit />
      </template>
      <SubscriptionEditDialog
        :subscription="subscription"
        @done="doneHandler()"
      />
    </PopButton>
    <button class="daisy-btn daisy-btn-sm" title="Unsubscribe" @click="processForm()">
      <SvgUnsubscribe />
    </button>
  </div>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue"
import type { Subscription } from "@generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import PopButton from "../commons/Popups/PopButton.vue"
import usePopups from "../commons/Popups/usePopups"
import SvgEdit from "../svgs/SvgEdit.vue"
import SvgUnsubscribe from "../svgs/SvgUnsubscribe.vue"
import SubscriptionEditDialog from "./SubscriptionEditDialog.vue"

export default defineComponent({
  setup() {
    return { ...useLoadingApi(), ...usePopups() }
  },
  props: {
    subscription: {
      type: Object as PropType<Subscription>,
      required: true,
    },
  },
  emits: ["updated"],
  components: { SvgUnsubscribe, PopButton, SvgEdit, SubscriptionEditDialog },
  methods: {
    async processForm() {
      if (
        await this.popups.confirm(`Confirm to unsubscribe from this notebook?`)
      ) {
        this.managedApi.services
          .destroySubscription({ subscription: this.subscription.id })
          .then(() => {
            this.$emit("updated")
          })
      }
    },
    doneHandler() {
      this.$emit("updated")
    },
  },
})
</script>
