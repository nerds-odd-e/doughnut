const loginOrRegister = () => {
  window.location = `/users/identify?from=${window.location.href}`;
};

export default loginOrRegister