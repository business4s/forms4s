import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const config: Config = {
    title: 'Forms4s',
    tagline: 'Automatically generated input forms for rapid development',
    favicon: 'img/favicon/favicon.ico',

    // GitHub pages deployment config.
    url: 'https://business4s.github.io/',
    baseUrl: '/forms4s/',
    organizationName: 'business4s',
    projectName: 'forms4s',
    trailingSlash: true,

    onBrokenLinks: 'throw',
    onBrokenMarkdownLinks: 'warn',

    // Even if you don't use internationalization, you can use this field to set
    // useful metadata like html lang. For example, if your site is Chinese, you
    // may want to replace "en" with "zh-Hans".
    i18n: {
        defaultLocale: 'en',
        locales: ['en'],
    },

    markdown: {
        mermaid: true,
    },
    themes: ['@docusaurus/theme-mermaid'],

    presets: [
        [
            'classic',
            {
                docs: {
                    sidebarPath: './sidebars.ts',
                    editUrl: 'https://github.com/business4s/forms4s/tree/main/website',
                    beforeDefaultRemarkPlugins: [
                        [
                            require('remark-code-snippets'),
                            {baseDir: "../forms4s-examples/src/"}
                        ]
                    ],
                },
                theme: {
                    customCss: './src/css/custom.css',
                },
            } satisfies Preset.Options,
        ],
    ],

    themeConfig: {
        // Replace with your project's social card
        image: 'img/docusaurus-social-card.jpg',
        navbar: {
            title: 'Forms4s',
            logo: {
                alt: 'Forms4s Logo',
                src: 'img/forms4s-logo.drawio.svg',
            },
            items: [
                {
                    type: 'docSidebar',
                    sidebarId: 'tutorialSidebar',
                    position: 'left',
                    label: 'Docs',
                },
                { to: '/demo', label: 'Demo', position: 'left' },
                {
                    href: 'https://github.com/business4s/forms4s',
                    label: 'GitHub',
                    position: 'right',
                },
            ],
        },
        footer: {
            style: 'dark',
            links: [
            ],
            // copyright: `Copyright © ${new Date().getFullYear()} My Project, Inc. Built with Docusaurus.`,
        },
        prism: {
            theme: prismThemes.github,
            darkTheme: prismThemes.dracula,
            additionalLanguages: ['java', 'scala', "json"]
        },
        // algolia: {
        //     appId: 'IMCN9UXKWU',
        //     apiKey: '6abd8b572e53e72a85a9283c552438b7',
        //     indexName: 'business4s',
        //     searchPagePath: 'search',
        // },
    } satisfies Preset.ThemeConfig,
    customFields: {
        forms4sVersion: process.env.FORMS4S_VERSION,
    },
};

export default config;
