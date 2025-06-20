var str = 'abc def'

// Array.prototype.push.call(str, ', pushed string') // TypeError: Cannot assign to read only property 'length' of object '[object String]'

Array.prototype.concat.call(str, 'string')

Array.prototype.every.call(str, function(char) {
  return char === ' '
})

Array.prototype.some.call(str, function(char) {
  return char === ' '
})

var newArr = Array.prototype.map.call(str, function(char) {
  return char + '!'
})

console.log(newArr)

/*
[
  'a!', 'b!',
  'c!', ' !',
  'd!', 'e!',
  'f!'
]

 */

var newStr = Array.prototype.reduce.apply(str, [
  function(string, char, i) {
  return string + char + i
  }, ''
])

console.log(newStr)

/*
a0b1c2 3d4e5f6
 */