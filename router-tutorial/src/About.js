import qs from "qs";

const About = ({ location }) => {
  // locaiton.search 역시 v6 부터는 사용할 수 없기 때문에 에러 발생
  // v6 부터는 params. location. history 를 사용할 수 없다고 한다.

  const query = qs.parse(location.search, {
    ignoreQueryPrefix: true// 이 설정을 통해 문자열 맨 앞의 ? 를 생략합니다/
  })
  const showDetail = query.detail === 'true' // 쿼리의 파싱 결과 값은 boolean이 아니라 문자열

  return (
    <div>
      <h1>소개</h1>
      <p>about 페이지 입니다.</p>
      <p>{showDetail && <p>detail 값을 true로 설정</p>}</p>
    </div>
  )
}

export default About;