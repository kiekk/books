import {expect} from 'chai';
import {statement} from '../../src/ch01/statement'

describe('statement', () => {
    let playsJson =
        {
            "hamlet": {"name": "Hamlet", "type": "tragedy"},
            "as-like": {"name": "As You Like It", "type": "comedy"},
            "othello": {"name": "Othello", "type": "tragedy"}
        };

    let invoicesJson =
        [
            {
                "customer": "BigCo",
                "performances": [
                    {
                        "playID": "hamlet",
                        "audience": 55
                    },
                    {
                        "playID": "as-like",
                        "audience": 35
                    },
                    {
                        "playID": "othello",
                        "audience": 40
                    }
                ]
            }
        ];

    it('should print a statement for multiple plays, single customer and multiple seats in plain text', () => {
        let expected = "청구 내역 (고객명: BigCo)\n" +
            " Hamlet : $2,900.00 (55 석)\n" +
            " As You Like It : $580.00 (35 석)\n" +
            " Othello : $1,400.00 (40 석)\n" +
            "총액: $4,880.00\n" +
            "적립 포인트: 47점\n";

        expect(statement(invoicesJson[0], playsJson)).to.equal(expected);
    });

});