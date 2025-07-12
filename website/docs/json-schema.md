---
sidebar_position: 4
---
# JSON Schema Integration

forms4s provides seamless integration with JSON Schema, allowing you to generate form elements directly from a JSON Schema definition. 
This makes it easy to create forms based on existing data models.


## Limitations

- Not all JSON Schema validation keywords are supported yet
- Complex schema combinations (allOf, anyOf) are not fully supported
- Custom formats may require additional configuration

For more advanced use cases, you may need to create form elements manually or extend the `FormFromJsonSchema` module.