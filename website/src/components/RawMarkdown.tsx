import React from 'react';

// build a context of all .md files under that folder:
const schemas = require.context(
    '!!raw-loader!../../../forms4s-examples/src/test/resources',
    true,
    /\.md$/
);

type Props = {
    file: string;
};

const SupportedJsonSchemaTypes: React.FC<Props> = ({ file }) => {
    let html = '';
    try {
        html = schemas(`./${file}`).default;
    } catch (e) {
        console.error(`Could not find ${file}`, e);
        return <div>⚠️ Schema not found: {file}</div>;
    }

    return <div dangerouslySetInnerHTML={{ __html: html }} />;
};

export default SupportedJsonSchemaTypes;