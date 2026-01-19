# Website

This website is built using [Docusaurus](https://docusaurus.io/), a modern static website generator.

## Local Development

### Quick start (without embedded demo)

```bash
cd website
yarn install
yarn start
```

This starts a local development server. The demo page will show a placeholder or broken state since the Tyrian example isn't built.

### Full setup (with embedded demo)

The website embeds a live Tyrian demo built from `forms4s-examples`. To run locally with the demo working:

```bash
# 1. Build the Scala.js example
cd forms4s-examples
yarn install
yarn build                          

# 2. Copy built assets to website
mkdir -p ../website/static/example-dist/
cp dist/* ../website/static/example-dist/

# 3. Start the website
cd ../website
yarn install
yarn start
```

For active development on the example, run `sbt ~forms4s-examplesJS/fastLinkJS` in one terminal and `yarn dev` in `forms4s-examples` in another, then copy the dist when ready.

## Build

```bash
yarn build
```

This generates static content into the `build` directory.

## Deployment

Deployment is automated via GitHub Actions on push to `main`. See `.github/workflows/website-deploy.yml`.
