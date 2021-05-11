<template>
    <div>
        <button class="region" v-for="region in regions" :key="region"
            :class="isSelected(region)"
            @click="changeRegion({region:region})"
        >
            {{ region }}
        </button>
    </div>
</template>

<script>
import Constant from '../Constant'
import { mapMutations, mapGetters } from 'vuex'

export default {
    name: 'RegionButtons',
    computed: {
        ...mapGetters(['regions', 'currentRegion'])
    },
    methods: {
        isSelected(region){
            if(region == this.currentRegion) {
                return {
                    selected: true
                }
            } else {
                return {
                    selected: false
                }
            }
        },
        ...mapMutations([
            Constant.CHANGE_REGION
        ])
    }
}
</script>

<style scoped>
    button.region {
        text-align: center;
        width: 80px;
        margin: 2px;
        border: 1px solid gray;
    }
    button.selected {
        background-color: purple;
        color: aqua;
    }
</style>