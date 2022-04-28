const loginOrRegister = async () => {
  window.location = `/users/identify?from=${window.location.href}`;
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  await new Promise(() => {}); // I promise ... Wait, why am I still here?
};

export default loginOrRegister;
