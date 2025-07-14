<table>
<thead>
<tr>
<th>FormElement Type</th>
<th>Empty Value</th>
<th>Example Value</th>
</tr>
</thead>
<tbody>
<tr>
<td>Text</td>
<td><pre><code class="language-json">""</code></pre></td>
<td><pre><code class="language-json">"foo"</code></pre></td>
</tr>
<tr>
<td>Number (Decimal)</td>
<td><pre><code class="language-json">null</code></pre></td>
<td><pre><code class="language-json">1.1</code></pre></td>
</tr>
<tr>
<td>Select</td>
<td><pre><code class="language-json">"Option 1"</code></pre></td>
<td><pre><code class="language-json">"Option 2"</code></pre></td>
</tr>
<tr>
<td>Checkbox</td>
<td><pre><code class="language-json">false</code></pre></td>
<td><pre><code class="language-json">true</code></pre></td>
</tr>
<tr>
<td>Group
<ul>
  <li>field1</li>
</ul></td>
<td><pre><code class="language-json">{
  "field1" : ""
}</code></pre></td>
<td><pre><code class="language-json">{
  "field1" : "foo"
}</code></pre></td>
</tr>
<tr>
<td>Multivalue</td>
<td><pre><code class="language-json">[
]</code></pre></td>
<td><pre><code class="language-json">[
  "foo"
]</code></pre></td>
</tr>
<tr>
<td>Alternative
<ul>
  
<li>
  alt1
  <ul>
    <li>field1</li>
  </ul>
</li>
           
<li>
  alt2
  <ul>
    <li>field1</li>
  </ul>
</li>
           
</ul></td>
<td><pre><code class="language-json">{
  "tpe" : "alt1",
  "field1" : ""
}</code></pre></td>
<td><pre><code class="language-json">{
  "tpe" : "alt2",
  "field1" : "foo"
}</code></pre></td>
</tr>
</tbody>
</table>