## 부록 C: DB로 분산 잠금 구현하기

### 분산 잠금 동작
- 분산 잠금이 필요한 스레드는 다음 절차에 따라 잠금을 획득한다.

1. 트랜잭션을 시작한다.
2. 선점 잠금 쿼리(for update)를 이용해 해당 행을 점유한다.
3. 행이 없으면 잠금 테이블에 새로운 데이터를 추가한다.
4. owner가 다른데 아직 expiry가 지나지 않았다면, 잠금 획득에 실패한다.
5. owner가 다른데 expiry가 지났다면, owner와 expiry 값을 변경한 후 잠금을 획득한다.
6. owner가 같다면 expiry만 갱신한 후 잠금을 획득한다.
7. 트랜잭션을 커밋하고 소유 결과를 리턴한다.
8. 트랜잭션 커밋에 실패하면 잠금 획득도 실패한다.

