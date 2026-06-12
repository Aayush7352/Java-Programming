package phase10.datastructures;

import java.util.StringJoiner;

class AVLNode {
    int value, height;
    AVLNode left, right;

    AVLNode(int value) {
        this.value = value;
        this.height = 1;
    }
}

class AVLTree {
    private AVLNode root;

    private int height(AVLNode node) {
        return node == null ? 0 : node.height;
    }

    private int balanceFactor(AVLNode node) {
        return node == null ? 0 : height(node.left) - height(node.right);
    }

    private void updateHeight(AVLNode node) {
        if (node != null) node.height = 1 + Math.max(height(node.left), height(node.right));
    }

    private AVLNode rotateRight(AVLNode y) {
        AVLNode x = y.left;
        AVLNode T = x.right;
        x.right = y;
        y.left = T;
        updateHeight(y);
        updateHeight(x);
        return x;
    }

    private AVLNode rotateLeft(AVLNode x) {
        AVLNode y = x.right;
        AVLNode T = y.left;
        y.left = x;
        x.right = T;
        updateHeight(x);
        updateHeight(y);
        return y;
    }

    public void insert(int value) {
        root = insert(root, value);
    }

    private AVLNode insert(AVLNode node, int value) {
        if (node == null) return new AVLNode(value);
        if (value < node.value) node.left = insert(node.left, value);
        else if (value > node.value) node.right = insert(node.right, value);
        else return node;

        updateHeight(node);
        return balance(node);
    }

    private AVLNode balance(AVLNode node) {
        int bf = balanceFactor(node);
        if (bf > 1) {
            if (balanceFactor(node.left) < 0) node.left = rotateLeft(node.left);
            return rotateRight(node);
        }
        if (bf < -1) {
            if (balanceFactor(node.right) > 0) node.right = rotateRight(node.right);
            return rotateLeft(node);
        }
        return node;
    }

    public String inorder() {
        StringJoiner sj = new StringJoiner(", ");
        inorder(root, sj);
        return sj.toString();
    }

    private void inorder(AVLNode node, StringJoiner sj) {
        if (node == null) return;
        inorder(node.left, sj);
        sj.add(String.valueOf(node.value));
        inorder(node.right, sj);
    }

    public void printHeights() {
        printHeights(root);
    }

    private void printHeights(AVLNode node) {
        if (node == null) return;
        printHeights(node.left);
        System.out.println("Node " + node.value + " height=" + node.height + " bf=" + balanceFactor(node));
        printHeights(node.right);
    }
}

public class AVLTreeExample {

    public static void main(String[] args) {
        AVLTree avl = new AVLTree();
        // Causes LL, RR, LR, RL rotations
        avl.insert(10);
        avl.insert(20);
        avl.insert(30);  // RR
        avl.insert(40);
        avl.insert(50);  // RR again
        avl.insert(25);  // RL
        avl.insert(5);
        avl.insert(3);   // LL
        avl.insert(7);   // LR

        System.out.println("Inorder: " + avl.inorder());
        System.out.println("\nHeights & balance factors:");
        avl.printHeights();
    }
}
