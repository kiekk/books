import { createContext, useContext } from 'react'

/*
  PreloadContext
  서버 사이드 렌더링을 하는 과정에서 처리해야할 작업들을 실행,
  프로미스가 있다면 프로미스를 수집
 
  useEffect나 클래스형의 componentDidMount는 서버 사이드 렌더링시 호출 되지 않기 때문
 */

// 클라이언트 환경: null
// 서버 환경 : { done: false, promises: [] }
const PreloadContext = createContext(null)
export default PreloadContext

// resolve는 함수 타입
export const Preloader = ({ resolve }) => {
  const preloadContext = useContext(PreloadContext)
  if (!preloadContext) {
    return null // context 값이 유효하지 않다면 아무것도 하지 않음
  }
  if (preloadContext.done) {
    return null // 이미 작업이 끝났다면 아무것도 하지 않음
  }

  // promises 배열에 프로미스 등록
  // resolve 함수가 프로미스를 반환하지 않더라도, 프로미스 취급을 위함
  // Promise.resolve 함수 사용
  preloadContext.promises.push(Promise.resolve(resolve()))
  return null
}
