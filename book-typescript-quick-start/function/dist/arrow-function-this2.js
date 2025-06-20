let typedPerson = {
    name: 'Happy',
    hello(name2) {
        return `Hello, ${this.name + name2}`;
    }
};
console.log(typedPerson.hello('World'));
