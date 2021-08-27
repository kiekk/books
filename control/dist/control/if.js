let text = '';
let statusActive = 0;
let isEnabled = true;
// first if
if (statusActive || text) {
    console.log('1');
}
// second if
if (isEnabled && 2 > 1) {
    console.log('2');
}
