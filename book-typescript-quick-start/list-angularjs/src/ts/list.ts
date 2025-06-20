import * as _ from 'underscore'

export class Item {
    constructor(public name: string, public point: number) {}
}

export class RecommendedPlaces {
    getBestPlace(items: Array<Item>): string {
        let bestPlace = _.max(items, i => i.point)
        // @ts-ignore
        return bestPlace.name
    }
}