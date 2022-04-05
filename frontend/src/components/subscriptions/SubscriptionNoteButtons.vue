<template>
  <div class="btn-group btn-group-sm">
    <PopupButton title="Edit subscription">
      <template v-slot:face>
        <SvgEdit />
      </template>
      <template #default="{ doneHandler }">
        <SubscriptionEditDialog :subscription="subscription" @done="doneHandler($event)" />
      </template>
    </PopupButton>
    <button class="btn btn-sm" title="Unsubscribe" @click="processForm()">
      <SvgUnsubscribe />
    </button>
  </div>
</template>

<script>
import SvgUnsubscribe from "../svgs/SvgUnsubscribe.vue";
import useLoadingApi from '../../managedApi/useLoadingApi';
import usePopups from "../commons/Popups/usePopup";
import PopupButton from "../commons/Popups/PopupButton.vue";
import SvgEdit from "../svgs/SvgEdit.vue";
import SubscriptionEditDialog from "./SubscriptionEditDialog.vue";

export default {
  setup() {
    return { ...useLoadingApi(), ...usePopups() };
  },
  props: { subscription: Object },
  emits: ["updated"],
  components: { SvgUnsubscribe, PopupButton, SvgEdit, SubscriptionEditDialog },
  methods: {
    async processForm() {
      if (
        await this.popups.confirm(
          `Confirm to unsubscribe from this notebook??`
        )
      ) {
        this.api.subscriptionMethods.deleteSubscription(this.subscription.id
        ).then((r) => {
          this.$emit("updated");
        });
      }
    },
  },
};
</script>
