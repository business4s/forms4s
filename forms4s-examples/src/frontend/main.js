import {TyrianApp} from 'scalajs:main.js'
import "bulma/css/bulma.min.css";

import bootstrapText from 'bootstrap/dist/css/bootstrap.min.css?inline';
import bulmaText from 'bulma/css/bulma.min.css?inline';
import picoText from '@picocss/pico/css/pico.min.css?inline';

customElements.define("css-separator", class extends HTMLElement {
    static get observedAttributes() {
        return ['renderer'];
    }

    #shadow = null;

    connectedCallback() {
        this.#shadow = this.attachShadow({ mode: "open" });
        while (this.firstChild) {
            this.#shadow.appendChild(this.firstChild);
        }
        this.#applyStyles();
    }

    attributeChangedCallback(name, oldValue, newValue) {
        if (name === 'renderer' && oldValue !== newValue && this.#shadow) {
            this.#applyStyles();
        }
    }

    #applyStyles() {
        const renderer = this.getAttribute("renderer");
        let cssText;

        if (renderer === "bootstrap") {
            cssText = bootstrapText.replace(/:root,\s*\[data-bs-theme=light\]/, ':host');
        } else if (renderer === "bulma") {
            cssText = bulmaText;
        } else if (renderer === "picocss") {
            cssText = picoText;
        } else if (renderer === "raw") {
            cssText = "";
        } else {
            throw new Error(`<css-separator> received unknown renderer '${renderer}'`);
        }

        const sheet = new CSSStyleSheet();
        sheet.replaceSync(cssText);
        this.#shadow.adoptedStyleSheets = [sheet];
    }
});

export { TyrianApp };