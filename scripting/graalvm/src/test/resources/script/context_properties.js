const parent = context.additional;
console.log('parent', parent);
console.log('parent json', JSON.stringify(parent));
console.log('parent keys', Object.keys(parent));
console.log('parent name', parent.name);

const child = parent.child;
console.log('child', child);
console.log('child json', JSON.stringify(child));
console.log('child keys', Object.keys(child));
console.log('child name', child.name);

respond().withContent(parent.name + ' ' + child.name);
