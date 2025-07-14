import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import React from 'react';
import CodeBlock from '@theme/CodeBlock';

const SbtDependencies: React.FC = () => {
    const {siteConfig} = useDocusaurusContext();
    const version = siteConfig.customFields?.forms4sVersion;
    return (
        <CodeBlock className="language-scala">
            {`libraryDependencies ++= Seq(
  "io.github.forms4s" %% "forms4s-core"       % "${version}", // Core functionality
  "io.github.forms4s" %% "forms4s-jsonschema" % "${version}", // JSON Schema support
  "io.github.forms4s" %% "forms4s-circe"      % "${version}", // JSON handling
  "io.github.forms4s" %% "forms4s-tyrian"     % "${version}"  // UI rendering
)`}
        </CodeBlock>
    );
}

export default SbtDependencies;