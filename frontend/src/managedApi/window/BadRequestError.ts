class BadRequestError extends Error {
  constructor(data) {
    super("Bad Request");
    Object.keys(data).forEach((key) => {
      if (key.includes(".")) {
        const [namespace, subkey] = key.split(".");
        if (!this[namespace]) this[namespace] = {};
        this[namespace][subkey] = data[key];
      } else {
        this[key] = data[key];
      }
    });
  }
}

export default BadRequestError;
