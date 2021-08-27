function colors(a, ...rest) {
    return a + ' ' + rest.join(' ');
}
let color1 = colors('red');
let color2 = colors('red', 'orange');
let color3 = colors('red', 'orange', 'yellow');
console.log(`
color1=${color1}
color2=${color2}
color3=${color3}    
`);
/*
    실행 결과
    color1=red
    color2=red orange
    color3=red orange yellow
 */ 
