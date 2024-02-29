export default function getEnvironment(): "production" | "testing" {
  return window.location.href.includes("odd-e.com") ? "production" : "testing";
}
