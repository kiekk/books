#include <stdio.h>
#include "BinaryTree3.h"	// 트리의 구조를 확인하기 위해서
#include "BinarySearchTree3.h"

int main(void)
{
	BTreeNode * avlRoot;
	BTreeNode * clNode;		// current left node
	BTreeNode * crNode;		// current right node

	BTreeNode * clNode2;    
	BTreeNode * crNode2;

	BTreeNode * clNode3;
	BTreeNode * crNode3;

	BSTMakeAndInit(&avlRoot);

	BSTInsert(&avlRoot, 1);
	BSTInsert(&avlRoot, 2);
	BSTInsert(&avlRoot, 3);
	BSTInsert(&avlRoot, 4);
	BSTInsert(&avlRoot, 5);
	BSTInsert(&avlRoot, 6);
	BSTInsert(&avlRoot, 7);
	BSTInsert(&avlRoot, 8);
	BSTInsert(&avlRoot, 9);

	printf("루트 노드: %d \n", GetData(avlRoot));    //4

	clNode = GetLeftSubTree(avlRoot);   //2, 루트 4의 왼편
	crNode = GetRightSubTree(avlRoot);  //6, 루트 4의 오른편
	printf("%d, %d \n", GetData(clNode), GetData(crNode));

	clNode2 = GetLeftSubTree(clNode);    //1, 2의 왼편
	crNode2 = GetRightSubTree(clNode);   //3, 2의 오른편
	printf("%d, %d \n", GetData(clNode2), GetData(crNode2));

	clNode2 = GetLeftSubTree(crNode);    //5, 3의 왼편
	crNode2 = GetRightSubTree(crNode);   //8, 3의 오른편
	printf("%d, %d \n", GetData(clNode2), GetData(crNode2));

	clNode3 = GetLeftSubTree(crNode2);   //7, 8의 왼편
	crNode3 = GetRightSubTree(crNode2);  //9, 8의 오른편
	printf("%d, %d \n", GetData(clNode3), GetData(crNode3)); 
	return 0;
}