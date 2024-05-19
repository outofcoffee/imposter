const store = stores.open('test');

// this should return a proxied array of proxied object elements
const example = store.load('example');

// length should be accurate
if (example.length !== 2) {
    throw new Error(`Invalid store item length: ${example.length}`);
}

// spread should work
const items = [...example];
items.push({ name: 'baz' });

let names = ''

// traversing as an array of objects should work
for (const item of items) {
    names += item.name + ",";
}

// JSON serialisation should work
const response = JSON.stringify(items);

respond()
    .withHeader('items', names)
    .withContent(response);
