var map = {};
map['one'] = 1;
map['one'] = 2;
map['one'] = 3;
for (var entry in map) {
    console.log(entry);
}
console.log(map['one']);
