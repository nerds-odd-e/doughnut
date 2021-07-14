import { h } from "vue"
import { RouterView } from "vue-router"
import { routerScopeGuard } from "../../routes/relative_routes"

function NestedPage(WrappedComponent, scopeName, exceptRoutes, notAllowedMessage){
  return  {
    name: 'NestedPage',
    computed: {
      isNested() {
        if(this.$route) {
          const routeParts = this.$route.name.split('-')
          return routeParts.length>1 && routeParts[1] != "quiz"
        }
      }
    },
    methods: {
      async alert() {
        return this.$popups.alert(notAllowedMessage)
      }

    },
    beforeRouteEnter(to, from, next) {next(vm=>routerScopeGuard(scopeName, exceptRoutes, vm.alert)(to, from, next))},
    beforeRouteUpdate(to, from, next) {routerScopeGuard(scopeName, exceptRoutes, this.alert)(to, from, next)},
    beforeRouteLeave(to, from, next) {routerScopeGuard(scopeName, exceptRoutes, this.alert)(to, from, next)},
    render() {
      return h('div', {}, [
        h(WrappedComponent, {...this.$props, nested: this.isNested}),
        h(RouterView, {}),
      ])
    }
  }
}

export default NestedPage