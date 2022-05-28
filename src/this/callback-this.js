// callback 함수를 인자로 받는 메소드 중 일부는 this를 직접 바인딩할 수 있습니다.

// 주로 배열 메소드에 많이 있으며, ES6의 Set, Map 등의 메소드에도 일부 존재합니다.

var report = {
  sum: 0,
  count: 0,
  add: function () {
    var args = Array.prototype.slice.call(arguments)
    args.forEach(function (entry) {
      this.sum += entry
      ++this.count
    }, this)
  },
  average: function () {
    return this.sum / this.count
  }
}

report.add(60, 85, 95)
console.log(report.sum, report.count, report.average());  // 240 3 80

/*
  callback 함수와 함께 thisArg를 인자로 받는 메소드
  Array.prototype.forEach(callback[, thisArg])
  Array.prototype.map(callback[, thisArg])
  Array.prototype.filter(callback[, thisArg])
  Array.prototype.som(callback[, thisArg])
  Array.prototype.every(callback[, thisArg])
  Array.prototype.find(callback[, thisArg])
  Array.prototype.findIndex(callback[, thisArg])
  Array.prototype.flatMap(callback[, thisArg])
  Array.prototype.from(callback[, thisArg])
  Set.prototype.forEach(callback[, thisArg])
  Map.prototype.forEach(callback[, thisArg])

 */