import React, { useState } from 'react'
import axios from 'axios'

const App = () => {
    const [data, setData] = useState(null)
    const onClick = async () => {
        try {
            // https://newsapi.org/register 에서 API key 발급
            const { data } = await axios.get('https://newsapi.org/v2/top-headlines?country=kr&apikey=aa592dd277fa4d5ab5b8e08ff5d4604c')
            setData(data)
        }catch(e) {
            console.log(e)
        }
    }
    
    return (
        <div>
            <div>
                <button onClick={onClick}>불러오기</button>
            </div>
            {data && <textarea rows={7} value={JSON.stringify(data, null, 2)} readOnly={true} />}
        </div>
    )
}

export default App;
