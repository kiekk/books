<template>
    <div>
        <hr class="divider" />
        <div>
            <table class="detail table table-bordered">
                <tbody>
                    <tr class="active">
                        <td>일련번호</td>
                        <td>{{ contact.no }}</td>
                    </tr>
                    <tr class="active">
                        <td>이름</td>
                        <td>{{ contact.name }}</td>
                    </tr>
                    <tr class="active">
                        <td>전화</td>
                        <td>{{ contact.tel }}</td>
                    </tr>
                    <tr class="active">
                        <td>주소</td>
                        <td>{{ contact.address }}</td>
                    </tr>
                </tbody>    
            </table>  
        </div>
    </div>
</template>

<script>
import contactlist from '../ContactList'

export default {
    name: 'contactbyno',
    props: ['no'],
    data(){
        return {
            contacts: contactlist.contacts
        }
    },
    computed: {
        contact(){
            var no = this.no
            var arr = this.contacts.filter((item) => item.no == no)

            if(arr.length === 1) {
                return arr[0]
            }else {
                return {}
            }
        }
    }
}
</script>

<style>
    table.detail {
        width: 400px;
    }
    .divider {
        height: 3px;
        margin-left: auto;
        margin-right: auto;
        background-color: #FF0066;
        color: #FF0066;
        border: 0 none;
    }
</style>