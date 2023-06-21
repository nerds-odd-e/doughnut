class BadRequestError extends Error {
  constructor(response) {
    const data = response.errors;
    super(response.message || "Bad Request");
    Object.keys(data).forEach((key) => {
      if (key.includes(".")) {
        const [namespace, subkey] = key.split(".");
        if (namespace !== undefined) {
          if (!this[namespace]) this[namespace] = {};
          this[namespace][subkey] = data[key];
        }
      } else {
        this[key] = data[key];
      }
    });
  }
}

export default BadRequestError;
