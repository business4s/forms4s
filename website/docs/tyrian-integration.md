---
sidebar_position: 5
---
import SbtDependency from '@site/src/components/SbtDependency';

# Tyrian Integration

forms4s is designed to be framework-agnostic, but it comes with Tyrian integration out of the box.

## Usage

<SbtDependency moduleName={"forms4s-tyrian"}/>

```scala file=./main/scala/forms4s/example/docs/TyrianExample.scala start=start_doc end=end_doc
```

## The FormRenderer Trait

The `FormRenderer` trait defines the interface for rendering form elements.

For the most usages one of the following renderers should be enough

- `BulmaFormRenderer` - Renders forms compatible with the [Bulma](https://bulma.io/) CSS framework
- `BootstrapFormRenderer` - Renders forms compatible with the [Bootstrap](https://getbootstrap.com/) CSS framework
- `RawFormRenderer` - Renders forms using no classes but only semantic HTML. That HTML is optimized to be compatible
  with [Pico](https://picocss.com/) CSS framework.

## Customizing Form Renderers

If you need to customize form rendering, you have two options:

1. For small modifications, you can extend one of the existing renderers and override specific methods to customize
   their behavior
2. For completely custom rendering, you can implement the `FormRenderer` trait from scratch to have full control over
   the rendering process.
   In such a case check existing renderers for inspiration.

