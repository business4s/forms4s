import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import React from 'react';
import CodeBlock from '@theme/CodeBlock';

interface SbtDependencyProps {
    moduleName: "forms4s-core";
}

const SbtDependency: React.FC<SbtDependencyProps> = ({moduleName}) => {
    const {siteConfig} = useDocusaurusContext();
    const version = siteConfig.customFields?.forms4sVersion;
    return (
        <CodeBlock className="language-scala">
            {`"org.business4s" %% "${moduleName}" % "${version}"`}
        </CodeBlock>
    );
}

export default SbtDependency;