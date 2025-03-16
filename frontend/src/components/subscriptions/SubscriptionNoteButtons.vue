<template>
  <div class="daisy:join">
    <PopButton title="Edit subscription">
      <template #button_face>
        <SvgEdit />
      </template>
      <SubscriptionEditDialog
        :subscription="subscription"
        @done="doneHandler($event)"
      />
    </PopButton>
    <button class="daisy:btn daisy:btn-sm" title="Unsubscribe" @click="processForm()">
      <SvgUnsubscribe />
    </button>
  </div>
</template>

<script>
import useLoadingApi from "@/managedApi/useLoadingApi"
import PopButton from "../commons/Popups/PopButton.vue"
import usePopups from "../commons/Popups/usePopups"
import SvgEdit from "../svgs/SvgEdit.vue"
import SvgUnsubscribe from "../svgs/SvgUnsubscribe.vue"
import SubscriptionEditDialog from "./SubscriptionEditDialog.vue"

export default {
  setup() {
    return { ...useLoadingApi(), ...usePopups() }
  },
  props: { subscription: Object },
  emits: ["updated"],
  components: { SvgUnsubscribe, PopButton, SvgEdit, SubscriptionEditDialog },
  methods: {
    async processForm() {
      if (
        await this.popups.confirm(`Confirm to unsubscribe from this notebook?`)
      ) {
        this.managedApi.restSubscriptionController
          .delete(this.subscription.id)
          .then(() => {
            this.$emit("updated")
          })
      }
    },
  },
}
</script>
