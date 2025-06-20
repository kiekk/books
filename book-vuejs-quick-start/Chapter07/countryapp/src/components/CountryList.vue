<template>
    <div id="example">
        <table id="list">
            <thead>
                <tr>
                    <th>번호</th>
                    <th>국가명</th>
                    <th>수도</th>
                    <th>지역</th>
                </tr>
            </thead>
            <tbody id="contacts">
                <th v-for="c in countries" :key="c.no">
                    <td>{{ c.no }}</td>
                    <td>{{ c.name }}</td>
                    <td>{{ c.capital }}</td>
                    <td>{{ c.region }}</td>
                </th>
            </tbody>
        </table>
    </div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
    name: 'CountryList',
    computed: {
        ...mapGetters({
            countries: 'countriesByRegion'
        })
    }
}
</script>

<style scoped>
    #list {
        width: 52px;
        border: 1px solid black;
        border-collapse: collapse;
    }
    #list td, #list th {
        border: 1px solid black;
        text-align: center;
    }
    #list td:nth-child(4n+1), #list th:htn-child(4n+1) {
        width: 100px;
    }
    #list td:nth-child(4n+2), #list th:htn-child(4n+2) {
        width: 150px;
    }
    #list td:nth-child(4n+3), #list th:htn-child(4n+3) {
        width: 150px;
    }
    #list td:nth-child(4n), #list th:htn-child(4n) {
        width: 120px;
    }
    #list > thead > tr {
        color: yellow;
        background-color: purple;
    }
</style>