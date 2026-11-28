/* CSP compatibility: replace the two CMS close-button inline handlers with delegated events. */
(() => {
  const descriptor = Object.getOwnPropertyDescriptor(Element.prototype, "innerHTML");
  if (!descriptor || typeof descriptor.set !== "function" || typeof descriptor.get !== "function") return;

  Object.defineProperty(Element.prototype, "innerHTML", {
    configurable: descriptor.configurable,
    enumerable: descriptor.enumerable,
    get: descriptor.get,
    set(value) {
      if (typeof value === "string") {
        value = value
          .replace(/onclick="this\.parentElement\.parentElement\.remove\(\)"/g, 'data-csp-close="sticky"')
          .replace(/onclick="this\.closest\('\.cms-dyn-ad'\)\.remove\(\)"/g, 'data-csp-close="popup"');
      }
      return descriptor.set.call(this, value);
    }
  });

  document.addEventListener("click", (event) => {
    const target = event.target;
    if (!(target instanceof Element)) return;
    const closeButton = target.closest("[data-csp-close]");
    if (!closeButton) return;
    const adContainer = closeButton.closest(".cms-dyn-ad");
    if (adContainer) adContainer.remove();
  });
})();
