<template>
  <div class="btn-group btn-group-sm">
    <SubscriptionEditButton :subscription="subscription" />
    <button class="btn btn-sm" title="Unsubscribe" @click="processForm()">
      <SvgUnsubscribe />
    </button>
  </div>
</template>

<script>
import SubscriptionEditButton from "./SubscriptionEditButton.vue";
import SvgUnsubscribe from "../svgs/SvgUnsubscribe.vue";
import { api } from "../../storedApi";

export default {
  props: { subscription: Object },
  emits: ["updated"],
  components: { SubscriptionEditButton, SvgUnsubscribe },
  methods: {
    async processForm() {
      if (
        await this.$popups.confirm(
          `Are yyou sure to unsubscribe from this notebook??`
        )
      ) {
        api().subscriptionMethods.deleteSubscription(this.subscription.id
        ).then((r) => {
          this.$emit("updated");
        });
      }
    },
  },
};
</script>
