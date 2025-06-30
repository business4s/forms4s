import React, {useEffect} from 'react';
import useBaseUrl from '@docusaurus/useBaseUrl';

export default function Forms4sDemo() {
    const cssUrl = useBaseUrl('/example-dist/index.css');
    const TyrianApp = require("@site/static/example-dist").TyrianApp;

    useEffect(() => {
        // Only register once
        if (customElements.get('forms4s-shadow')) return;

        class Forms4sElement extends HTMLElement {
            constructor() {
                super();
                const shadow = this.attachShadow({mode: 'open'});

                // Load styles
                const link = document.createElement('link');
                link.rel = 'stylesheet';
                link.href = cssUrl;
                shadow.appendChild(link);

                // Placeholder div for your Vite app to render into
                const container = document.createElement('div');
                container.id = 'forms4s-demo';
                shadow.appendChild(container);
                TyrianApp.launch(container);
            }
        }

        customElements.define('forms4s-shadow', Forms4sElement);
    }, [cssUrl]);

    return <forms4s-shadow/>;
}