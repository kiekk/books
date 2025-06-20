function myIndexOf2(x, y) {
    if (typeof x === 'string') {
        return x.indexOf(y); // x가 string일 때
    }
    else {
        return -1; // x가 number일 때
    }
}
console.log(myIndexOf2('hello', 'e'));
