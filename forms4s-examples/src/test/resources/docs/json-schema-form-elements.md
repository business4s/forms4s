<table>
<thead>
<tr>
<th>Description</th>
<th>JSON Schema</th>
<th>Form Element Type</th>
</tr>
</thead>
<tbody>
<tr>
<td>Basic text field</td>
<td><pre><code class="language-json">{
  "type" : "string"
}</code></pre></td>
<td>Text</td>
</tr>
<tr>
<td>Multiline text field</td>
<td><pre><code class="language-json">{
  "type" : "string",
  "format" : "multiline"
}</code></pre></td>
<td>Text (Multiline)</td>
</tr>
<tr>
<td>Date field</td>
<td><pre><code class="language-json">{
  "type" : "string",
  "format" : "date"
}</code></pre></td>
<td>Text (Date)</td>
</tr>
<tr>
<td>Time field</td>
<td><pre><code class="language-json">{
  "type" : "string",
  "format" : "time"
}</code></pre></td>
<td>Text (Time)</td>
</tr>
<tr>
<td>Date and time field</td>
<td><pre><code class="language-json">{
  "type" : "string",
  "format" : "date-time"
}</code></pre></td>
<td>Text (DateTime)</td>
</tr>
<tr>
<td>Email field</td>
<td><pre><code class="language-json">{
  "type" : "string",
  "format" : "email"
}</code></pre></td>
<td>Text (Email)</td>
</tr>
<tr>
<td>Integer number field</td>
<td><pre><code class="language-json">{
  "type" : "integer"
}</code></pre></td>
<td>Number (Integer)</td>
</tr>
<tr>
<td>Decimal number field</td>
<td><pre><code class="language-json">{
  "type" : "number"
}</code></pre></td>
<td>Number (Decimal)</td>
</tr>
<tr>
<td>Boolean checkbox field</td>
<td><pre><code class="language-json">{
  "type" : "boolean"
}</code></pre></td>
<td>Checkbox</td>
</tr>
<tr>
<td>Array of form elements</td>
<td><pre><code class="language-json">{
  "type" : "array",
  "items" : {
    "type" : "string"
  }
}</code></pre></td>
<td>Multivalue</td>
</tr>
<tr>
<td>Dropdown selection field</td>
<td><pre><code class="language-json">{
  "type" : "string",
  "enum" : [
    "Option 1",
    "Option 2",
    "Option 3"
  ]
}</code></pre></td>
<td>Select</td>
</tr>
<tr>
<td>Group of form elements</td>
<td><pre><code class="language-json">{
  "type" : "object",
  "properties" : {
    "field1" : {
      "type" : "string"
    },
    "field2" : {
      "type" : "string"
    }
  }
}</code></pre></td>
<td>Group
<ul>
  <li>field1</li><li>field2</li>
</ul></td>
</tr>
<tr>
<td>Alternative form elements</td>
<td><pre><code class="language-json">{
  "oneOf" : [
    {
      "title" : "Option 1",
      "type" : "object",
      "properties" : {
        "field1" : {
          "type" : "string"
        }
      }
    },
    {
      "title" : "Option 2",
      "type" : "object",
      "properties" : {
        "field2" : {
          "type" : "string"
        }
      }
    }
  ]
}</code></pre></td>
<td>Alternative
<ul>
  
<li>
  Option 1
  <ul>
    <li>field1</li>
  </ul>
</li>
           
<li>
  Option 2
  <ul>
    <li>field2</li>
  </ul>
</li>
           
</ul></td>
</tr>
</tbody>
</table>