import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import React from 'react';
import CodeBlock from '@theme/CodeBlock';

const SbtDependencies: React.FC = () => {
    const {siteConfig} = useDocusaurusContext();
    const version = siteConfig.customFields?.forms4sVersion;
    return (
        <CodeBlock className="language-scala">
            {`libraryDependencies ++= Seq(
  "org.business4s" %%  "forms4s-core"       % "${version}", // Core functionality
  "org.business4s" %%  "forms4s-circe"      % "${version}", // JSON handling
  "org.business4s" %%% "forms4s-tyrian"     % "${version}"  // UI rendering
)`}
        </CodeBlock>
    );
}

export default SbtDependencies;
