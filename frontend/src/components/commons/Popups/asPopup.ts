import usePopups from "./usePopups";

function asPopup() {
  return {
    popup: {
      done(result: unknown) {
        usePopups().popups.done(result);
      },
    },
  };
}

export default asPopup;
