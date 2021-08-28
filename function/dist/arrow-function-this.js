let person = {
    name: 'Happy',
    hello(name2) {
        console.log('Hello :', this.name, name2);
    }
};
person.hello('World');
