import { PropType, defineComponent, h, VNode } from "vue"
import { RouterView } from "vue-router"
import { User } from "@/generated/backend"
import usePopups from "../../components/commons/Popups/usePopups"
import routerScopeGuard from "../../routes/relative_routes"
import { StorageAccessor } from "../../store/createNoteStorage"

interface NestedPageProps {
  storageAccessor: StorageAccessor
  user: User
}

function NestedPage(
  WrappedComponent: ReturnType<typeof defineComponent>,
  scopeName: string
) {
  return defineComponent({
    name: "NestedPage",
    setup() {
      return usePopups()
    },
    props: {
      storageAccessor: {
        type: Object as PropType<StorageAccessor>,
        required: true,
      },
      user: {
        type: Object as PropType<User>,
        required: true,
      },
    },
    computed: {
      isNested(): boolean {
        if (this.$route?.name) {
          const routeParts = this.$route.name.toString().split("-")
          return routeParts.length > 1
        }
        return true
      },
    },
    beforeRouteEnter(to, _from, next) {
      routerScopeGuard(scopeName)(to, next)
    },
    beforeRouteUpdate(to, _from, next) {
      routerScopeGuard(scopeName)(to, next)
    },
    beforeRouteLeave(to, _from, next) {
      routerScopeGuard(scopeName)(to, next)
    },
    render(): VNode {
      const props: NestedPageProps = {
        storageAccessor: this.storageAccessor,
        user: this.user,
      }
      return h("div", { class: "content" }, [
        h(WrappedComponent, { ...props, minimized: this.isNested }),
        h(RouterView, props),
      ])
    },
  })
}

export default NestedPage
