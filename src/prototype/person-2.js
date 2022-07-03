var Person = function (name) {
  this._name = name
}

Person.prototype.getName = function () {
  return this._name
}

var suzi = new Person('suzi')

suzi.__proto__._name = 'SUZI__proto__'

console.log(suzi.__proto__.getName()) // SUZI__proto__
