const nonBlockingPopup = (urlGetter: Promise<string>) => {
  const popupWindow = window.open("");
  if (popupWindow) {
    urlGetter.then((url) => {
      popupWindow.location.href = url;
      popupWindow.focus();
    });
  }
};

export default nonBlockingPopup;
