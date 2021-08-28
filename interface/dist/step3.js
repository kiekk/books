class Person3 {
    constructor(name, city) {
        this.name = name;
        this.city = city;
        this.full = `${name}(${city})`;
    }
}
let person3 = [
    new Person3('kim', 'seoul'),
    new Person3('park', 'daejeon'),
    new Person3('jeong', 'daegu')
];
console.log(JSON.stringify(person3));
/*
    실행 결과
    [{"name":"kim","city":"seoul","full":"kim(seoul)"},{"name":"park","city":"daejeon","full":"park(daejeon)"},{"name":"jeong","city":"daegu","full":"jeong(
    daegu)"}]
 */ 
