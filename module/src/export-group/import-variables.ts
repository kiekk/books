import { ver, author, extensions, display } from "./export-variables";
console.log(`API Version : ${ver}`)
console.log(`API Name : ${author}`)

extensions.forEach(s => {
    console.log(`extensions : ${s}`)
})
console.log(`display() : ${display()}`)

/*
    실행 결과
    API Version : 1.0
    API Name : happy
    extensions : jpg
    extensions : bmp
    extensions : png
    display() : hello world
 */