import React, { useEffect } from 'react';
import useBaseUrl from '@docusaurus/useBaseUrl';

export default function Forms4sDemo() {
    const jsUrl = useBaseUrl('/example-dist/index.js');
    const cssUrl = useBaseUrl('/example-dist/index.css');

    useEffect(() => {
        // Only register once
        if (customElements.get('forms4s-shadow')) return;

        class Forms4sElement extends HTMLElement {
            constructor() {
                super();
                const shadow = this.attachShadow({ mode: 'open' });

                // Load styles
                const link = document.createElement('link');
                link.rel = 'stylesheet';
                link.href = cssUrl;
                shadow.appendChild(link);

                // Placeholder div for your Vite app to render into
                const container = document.createElement('div');
                container.id = 'forms4s-demo';
                shadow.appendChild(container);

                const script = document.createElement('script');
                script.type = 'module';
                script.src = jsUrl;
                script.src = jsUrl;
                script.onload = () => {
                    window.TyrianApp.launch(container)
                };
                shadow.appendChild(script);
            }
        }

        customElements.define('forms4s-shadow', Forms4sElement);
    }, [jsUrl, cssUrl]);

    return <forms4s-shadow />;
}