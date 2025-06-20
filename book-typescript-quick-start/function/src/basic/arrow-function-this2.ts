interface PersonType {
    name: string,
    hello(this: PersonType, name2: string): string
}

let typedPerson: PersonType = {
    name: 'Happy',
    hello(this: PersonType, name2: string): string {
        return `Hello, ${this.name + name2}`
    }
}

console.log(typedPerson.hello('World'))
