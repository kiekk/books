let list: Map<number, string> = new Map<number, string>()

list.set(1, 'one')
list.set(2, 'two')
list.set(3, 'three')

console.log(list)
// Map(3) { 1 => 'one', 2 => 'two', 3 => 'three' }

if(list.delete(2)) {
    console.log(list)
    // Map(2) { 1 => 'one', 3 => 'three' }
}

list.clear()
console.log(list)
// Map(0) {}
