const loggerMiddleware = (store) => (next) => (action) => {
  console.group(action && action.type); // 액션 타입으로 log를 group화 함
  console.log('이전 상태', store.getState());
  console.log('액션', action);
  next(action); // 다음 미들웨어, 리듀서로 전달
  console.log('다음 상태', store.getState());
  console.groupEnd();
};

export default loggerMiddleware;
