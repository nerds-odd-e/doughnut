const loginOrRegister = async () => {
  window.location = `/users/identify?from=${window.location.href}`;
  await new Promise(()=>{}); // I promise ... Wait, why am I still here?
};

export default loginOrRegister;
