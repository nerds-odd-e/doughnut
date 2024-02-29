import ManagedApi from "./ManagedApi";

export const timezoneParam = () => {
  const { timeZone } = Intl.DateTimeFormat().resolvedOptions();
  return timeZone;
};

const apiCollection = (managedApi: ManagedApi) => ({
  userMethods: {
    logout() {
      return managedApi.restPostWithHtmlResponse(`/logout`, {});
    },
  },

  testability: {
    getEnvironment() {
      return window.location.href.includes("odd-e.com")
        ? "production"
        : "testing";
    },
  },
});

export default apiCollection;
