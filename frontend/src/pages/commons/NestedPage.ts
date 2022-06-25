import { defineComponent, h } from "vue";
import { RouterView } from "vue-router";
import usePopups from "../../components/commons/Popups/usePopup";
import routerScopeGuard from "../../routes/relative_routes";

function NestedPage(
  WrappedComponent: ReturnType<typeof defineComponent>,
  scopeName: string
) {
  return defineComponent({
    name: "NestedPage",
    setup() {
      return usePopups();
    },
    computed: {
      isNested() {
        if (this.$route) {
          const routeParts = this.$route?.name?.toString().split("-");
          return (
            routeParts && routeParts.length > 1 && routeParts[1] !== "quiz"
          );
        }
        return true;
      },
    },
    beforeRouteEnter(to, _from, next) {
      next(() => routerScopeGuard(scopeName)(to, next));
    },
    beforeRouteUpdate(to, _from, next) {
      routerScopeGuard(scopeName)(to, next);
    },
    beforeRouteLeave(to, _from, next) {
      routerScopeGuard(scopeName)(to, next);
    },
    render() {
      return h("div", { class: "inner-box" }, [
        h(WrappedComponent, { ...this.$props, nested: this.isNested }),
        h(RouterView, {}),
      ]);
    },
  });
}

export default NestedPage;
