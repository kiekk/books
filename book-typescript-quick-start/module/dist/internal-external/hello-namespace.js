var Hello;
(function (Hello) {
    function print() {
        console.log('Hello');
    }
})(Hello || (Hello = {}));
