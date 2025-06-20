let a = 2

;(function () {
  console.log(a)

  let a = 2
})()

// 2022-04-14 추가
// let, const 는 hoisting 이 허용되지 않는다.
// const 는 변수의 선언과 할당이 동시에 이루어져야 하는데, hoisting 은 선언부만 별도로 분리하여 상단으로
// 끌어올리기 때문에 불가능하다.
// let 은 조금 다른데, let 을 이해하기 위해서는 TDZ(Temporal Dead Zone)을 이해해야 한다.

/*
  상단 코드를 실행하면 아래와 같은 에러가 발생한다
  E:\study\study-core-javascript\src\hosting\let-const.js:4
  console.log(a)
              ^

  ReferenceError: Cannot access 'a' before initialization
      at E:\study\study-core-javascript\src\hosting\let-const.js:4:15
      at Object.<anonymous> (E:\study\study-core-javascript\src\hosting\let-const.js:7:3)
  ?[90m    at Module._compile (internal/modules/cjs/loader.js:1085:14)?[39m
  ?[90m    at Object.Module._extensions..js (internal/modules/cjs/loader.js:1114:10)?[39m
  ?[90m    at Module.load (internal/modules/cjs/loader.js:950:32)?[39m
  ?[90m    at Function.Module._load (internal/modules/cjs/loader.js:790:14)?[39m
  ?[90m    at Function.executeUserEntryPoint [as runMain] (internal/modules/run_main.js:76:12)?[39m
  ?[90m    at internal/main/run_main_module.js:17:47?[39m

 */

/*
  아래 코드를 실행하면 아래와 같은 에러가 발생한다.
  console.log(c);
            ^

  ReferenceError: c is not defined
      at Object.<anonymous> (E:\study\study-core-javascript\src\hosting\let-const.js:38:13)
  ?[90m    at Module._compile (internal/modules/cjs/loader.js:1085:14)?[39m
  ?[90m    at Object.Module._extensions..js (internal/modules/cjs/loader.js:1114:10)?[39m
  ?[90m    at Module.load (internal/modules/cjs/loader.js:950:32)?[39m
  ?[90m    at Function.Module._load (internal/modules/cjs/loader.js:790:14)?[39m
  ?[90m    at Function.executeUserEntryPoint [as runMain] (internal/modules/run_main.js:76:12)?[39m
  ?[90m    at internal/main/run_main_module.js:17:47?[39m

 */
/*
  변수 c 에러의 경우는 scope 로 인해 c를 찾지 못한다는 에러이지만,
  변수 a 에러의 경우는 초기화 하기 전까지는 접근할 수 없다는 에러가 발생한다.

  이것이 바로 TDZ(Temporal Dead Zone)이다.
  변수가 선언은 됐지만 할당이 되기 전까지는 TDZ 가 되는 것이다.
  따라서 let 의 경우에는 선언, 할당을 분리해도 되지만 할당이 되기 전까지는
  let 변수를 호출하는 것을 금지하고 있다.

  따라서 결국 let, const 는 hoisting 이 되지 않는다.
 */
function b() {

  let c = 2
}
b()
console.log(c);
//
// function d() {
//   console.log(e)
//
//   const e = 3
// }
// d()
