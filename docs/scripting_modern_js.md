# Using modern JavaScript features in scripts

The default JavaScript engine is GraalVM, which is based on ECMAScript 2022 (more formally, [ECMA-262, 13th edition](https://262.ecma-international.org/13.0/)). This means you can use modern JavaScript features from ECMAScript 2022 in your scripts.

### Features

GraalVM enables you to use modern JavaScript features such as:

- `let` and `const` for variable declarations
- Arrow functions
- Template literals
- Destructuring
- Classes

To use the GraalVM JavaScript engine, you need to be running Imposter v4.0.0 or later.

---

## Examples

For examples, see the `examples/graal` directory [in GitHub](https://github.com/imposter-project/examples/blob/main/graal).

### Simple example

Start the mock server:

```bash
imposter up examples/graal/simple
```

Send a request to the mock server:

```bash
curl -i http://localhost:8080?name=Ada

Hello Ada
```

### Advanced example

See the `examples/graal/es6` [directory](https://github.com/imposter-project/examples/blob/main/graal) for an example of using modern JavaScript features in a script.

---

## Further reading

- [Using legacy JavaScript engine (Nashorn)](scripting_legacy_js.md)
