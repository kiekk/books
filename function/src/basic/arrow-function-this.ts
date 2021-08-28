let person = {
    name: 'Happy',
    hello(name2: string){
        console.log('Hello :', this.name, name2)
    }
}

person.hello('World')