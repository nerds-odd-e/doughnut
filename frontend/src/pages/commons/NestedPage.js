import { h } from "vue"
import RelativeRouterView from "../../routes/RelativeRouterView.vue"
import { routerScopeGuard } from "../../routes/relative_routes"

function NestedPage(WrappedComponent, scopeName, exceptRoutes, navigateOutWarningMessage){
  return  {
    name: 'NestedPage',
    components: { RelativeRouterView },
    computed: {
      isNested() {
        if(this.$route) {
          return this.$route.name.split('-').length>1
        }
      }
    },
    beforeRouteEnter(to, from, next) {routerScopeGuard(scopeName, exceptRoutes, navigateOutWarningMessage)(to, from, next)},
    beforeRouteUpdate(to, from, next) {routerScopeGuard(scopeName, exceptRoutes, navigateOutWarningMessage)(to, from, next)},
    beforeRouteLeave(to, from, next) {routerScopeGuard(scopeName, exceptRoutes, navigateOutWarningMessage)(to, from, next)},
    render() {
      return h('div', {}, [
        h(WrappedComponent, {...this.$props, nested: this.isNested}),
        h(RelativeRouterView, {}),
      ])
    }
  }
}

export default NestedPage