/**
 * Exposes the @asciidoctor/core browser bundle on globalThis for write.js.
 */
if (typeof module$build$asciidoctor_browser !== 'undefined') {
    globalThis.Asciidoctor = module$build$asciidoctor_browser.default;
}
