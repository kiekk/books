import {createContext, useContext} from "react";

const PreloadContext = createContext(null)

export default PreloadContext

export const Preloader = ({resolve}) => {
  const preloadContext = useContext(PreloadContext)

  // context 값이 유효하지 않을 경우 아무 작업도 하지 않음
  if (!preloadContext) {
    return null
  }
  // 이미 작업이 끝났다면 아무 작업도 하지 않음
  if (preloadContext.done) {
    return null
  }

  preloadContext.promises.push(Promise.resolve(resolve()))
  return null
}