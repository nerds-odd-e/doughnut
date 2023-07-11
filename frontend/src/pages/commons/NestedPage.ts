import { defineComponent, h, PropType } from "vue";
import { RouterView } from "vue-router";
import usePopups from "../../components/commons/Popups/usePopups";
import routerScopeGuard from "../../routes/relative_routes";
import { StorageAccessor } from "../../store/createNoteStorage";

function NestedPage(
  WrappedComponent: ReturnType<typeof defineComponent>,
  scopeName: string,
) {
  return defineComponent({
    name: "NestedPage",
    setup() {
      return usePopups();
    },
    props: {
      storageAccessor: {
        type: Object as PropType<StorageAccessor>,
        required: true,
      },
    },
    computed: {
      isNested() {
        if (this.$route) {
          const routeParts = this.$route?.name?.toString().split("-");
          return routeParts && routeParts.length > 1;
        }
        return true;
      },
    },
    beforeRouteEnter(to, _from, next) {
      routerScopeGuard(scopeName)(to, next);
    },
    beforeRouteUpdate(to, _from, next) {
      routerScopeGuard(scopeName)(to, next);
    },
    beforeRouteLeave(to, _from, next) {
      routerScopeGuard(scopeName)(to, next);
    },
    render() {
      return h("div", { class: "content" }, [
        h("div", { class: "inner-box" }, [
          h(WrappedComponent, { ...this.$props, minimized: this.isNested }),
          h(RouterView, { ...this.$props }),
        ]),
      ]);
    },
  });
}

export default NestedPage;
