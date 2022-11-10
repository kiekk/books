#include <stdio.h>

void HanioTowerMove(int num, char from, char by, char to) {
	if (num == 1) {
		printf("원반 1을 %c에서 %c로 이동 \n", from, to);
	}
	else {
		HanioTowerMove(num - 1, from, to, by);
		printf("원반 %d을(를) %c에서 %c로 이동 \n", num, from, to);
		HanioTowerMove(num - 1, by, from, to);
	}
}

int main(void) {
	HanioTowerMove(3, 'A', 'B', 'C');
	return 0;
}