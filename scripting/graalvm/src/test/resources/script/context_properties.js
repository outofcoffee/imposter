const parent = context.additional;
console.log('parent', parent);
console.log('parent json', JSON.stringify(parent));
console.log('parent keys', Object.keys(parent));

const parentName = parent.name;
console.log('parentName', parentName);
console.log('parentName json', JSON.stringify(parentName));
console.log('parentName keys', Object.keys(parentName));

const child = parent.child;
console.log('child', child);
console.log('child json', JSON.stringify(child));
console.log('child keys', Object.keys(child));

const childName = child.name;
console.log('childName', childName);
console.log('childName json', JSON.stringify(childName));
console.log('childName keys', Object.keys(childName));

respond().withContent(parentName + ' ' + childName);
