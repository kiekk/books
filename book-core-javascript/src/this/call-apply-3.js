function Person(name, gender) {
  this.name = name;
  this.gender = gender;
}

function Student(name, gender, school) {
  Person.call(this, name, gender);
  this.school = school
}

function Employee(name, gender, company) {
  Person.apply(this, [name, gender])
  this.company = company
}

var by = new Student('보영', 'female', '단국대')
var jn = new Employee('재난', 'male', '구골')

console.log(by)
console.log(jn)