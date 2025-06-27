import clsx from 'clsx';
import Heading from '@theme/Heading';
import styles from './styles.module.css';

type FeatureItem = {
    title: string;
    Svg?: React.ComponentType<React.ComponentProps<'svg'>>;
    description: JSX.Element;
};

const FeatureList: FeatureItem[] = [
    {
        title: 'Automatic',
        description: (
            <>
                Forms4s derives form structure from your Scala types, so your UI always matches your
                model.
            </>
        ),
    },
    {
        title: 'Flexible',
        description: (
            <>
                Forms4s is designed to allow for integration with any UI framework or json library.
            </>
        ),
    },
    {
        title: 'Powerful',
        description: (
            <>
                Forms4s supports user interaction, validation, dynamic updates and extracting structured JSON from the
                UI, and more.
            </>
        ),
    },
];

function Feature({title, Svg, description}: FeatureItem) {
    return (
        <div className={clsx('col col--4')}>
            {/*<div className="text--center">*/}
            {/*  <Svg className={styles.featureSvg} role="img" />*/}
            {/*</div>*/}
            <div className="text--center padding-horiz--md">
                <Heading as="h3">{title}</Heading>
                <p>{description}</p>
            </div>
        </div>
    );
}

export default function HomepageFeatures(): JSX.Element {
    return (
        <section className={styles.features}>
            <div className="container">
                <div className="row">
                    {FeatureList.map((props, idx) => (
                        <Feature key={idx} {...props} />
                    ))}
                </div>
            </div>
        </section>
    );
}
