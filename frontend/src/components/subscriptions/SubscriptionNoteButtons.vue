<template>
<div class="btn-group btn-group-sm">
    <SubscriptionEditButton :subscription="subscription" />
      <button class="btn btn-sm" title="Unsubscribe" @click="processForm()">
          <SvgUnsubscribe/>
      </button>
</div>

</template>

<script >
import SubscriptionEditButton from "./SubscriptionEditButton.vue"
import SvgUnsubscribe from "../svgs/SvgUnsubscribe.vue"
import { restPost } from "../../restful/restful"

export default {
  props: { subscription: Object },
  emits: ['updated'],
  components: { SubscriptionEditButton, SvgUnsubscribe },
  methods: {
      async processForm() {
          if(await this.$popups.confirm(`Are yyou sure to unsubscribe from this notebook??`)) {
              restPost(`/api/subscriptions/${this.subscription.id}/delete`, {}, r=>{})
                .then( r=>{ this.$emit('updated') })
          }
      }
  }
}

</script>
