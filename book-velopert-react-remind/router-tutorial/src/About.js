import qs from "qs";
import {useLocation} from "react-router-dom";


const About = () => {
  const { search } = useLocation();

  const query = qs.parse(search, {
    ignoreQueryPrefix: true// 이 설정을 통해 문자열 맨 앞의 ? 를 생략합니다/
  })
  const showDetail = query.detail === 'true' // 쿼리의 파싱 결과 값은 boolean이 아니라 문자열

  return (
    <div>
      <h1>소개</h1>
      <p>about 페이지 입니다.</p>
      {/*
        <p>{showDetail && <p>detail 값을 true로 설정</p>}</p>
        이렇게 작성하면 p 태그가 중첩되어 에러가 발생하는 듯 하다.

        validateDOMNesting(...): <p> cannot appear as a descendant of <p>.
      */}
      {showDetail && <p>detail 값을 true로 설정</p>}
    </div>
  )
}

export default About;