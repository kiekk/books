import React from 'react'

const data = {
    velopert: {
        name: '김수박',
        description: '수박을 좋아하는 사람'
    },
    gildong: {
        name: '홍길동',
        description: '홍길동전 주인공'
    }
}

const Profile = ({ match }) => {
    const { username } = match.params
    const profile = data[username]
    if(!profile) {
        return (
            <div>존재하지 않는 사용자입니다.</div>
        )
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

export default Profile