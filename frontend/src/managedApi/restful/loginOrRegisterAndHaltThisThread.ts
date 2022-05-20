const loginOrRegisterAndHaltThisThread = async () => {
  window.location.href = `/users/identify?from=${window.location.href}`;
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  await new Promise(() => {}); // I promise ... Wait, why am I still here?
};

export default loginOrRegisterAndHaltThisThread;
