import {useParams} from "react-router";

const data = {
  testUser: {
    name: 'test',
    description: 'test user'
  },
  gildong: {
    name: '홍길동',
    description: '고전 소설 홍길동전 주인공'
  }
}

const Profile = () => {
  const {username} = useParams();
  // react v6 부터는 props.match.params.username 처럼 가져올 수 없다고 한다.
  // 대신 useParams()를 사용하면 된다.
  const profile = data[username];

  if(!profile) {
    return <div>존재하지 않는 사용자입니다.</div>
  }

  return (
    <div>
      <h3>
        {username} ({profile.name})
      </h3>
      <p>{profile.description}</p>
    </div>
  )
}

export default Profile;