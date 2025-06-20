var MyAnimal;
(function (MyAnimal) {
    var Land;
    (function (Land) {
        function run() {
            console.log('육상 동물이 달립니다.');
        }
        Land.run = run;
    })(Land = MyAnimal.Land || (MyAnimal.Land = {}));
})(MyAnimal || (MyAnimal = {}));
(function (MyAnimal) {
    MyAnimal.Land.run();
})(MyAnimal || (MyAnimal = {}));
