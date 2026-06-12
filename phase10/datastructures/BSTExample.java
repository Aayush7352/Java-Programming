package phase10.datastructures;

import java.util.StringJoiner;

class BSTNode {
    int value;
    BSTNode left, right;

    BSTNode(int value) {
        this.value = value;
    }
}

class BST {
    private BSTNode root;

    public void insert(int value) {
        root = insert(root, value);
    }

    private BSTNode insert(BSTNode node, int value) {
        if (node == null) return new BSTNode(value);
        if (value < node.value) node.left = insert(node.left, value);
        else if (value > node.value) node.right = insert(node.right, value);
        return node;
    }

    public boolean search(int value) {
        return search(root, value);
    }

    private boolean search(BSTNode node, int value) {
        if (node == null) return false;
        if (value == node.value) return true;
        return value < node.value ? search(node.left, value) : search(node.right, value);
    }

    public void delete(int value) {
        root = delete(root, value);
    }

    private BSTNode delete(BSTNode node, int value) {
        if (node == null) return null;
        if (value < node.value) node.left = delete(node.left, value);
        else if (value > node.value) node.right = delete(node.right, value);
        else {
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;
            BSTNode min = findMin(node.right);
            node.value = min.value;
            node.right = delete(node.right, min.value);
        }
        return node;
    }

    public int findMin() {
        if (root == null) throw new IllegalStateException("Tree empty");
        return findMin(root).value;
    }

    private BSTNode findMin(BSTNode node) {
        while (node.left != null) node = node.left;
        return node;
    }

    public int findMax() {
        if (root == null) throw new IllegalStateException("Tree empty");
        BSTNode node = root;
        while (node.right != null) node = node.right;
        return node.value;
    }

    public String inorder() {
        StringJoiner sj = new StringJoiner(", ");
        inorder(root, sj);
        return sj.toString();
    }

    private void inorder(BSTNode node, StringJoiner sj) {
        if (node == null) return;
        inorder(node.left, sj);
        sj.add(String.valueOf(node.value));
        inorder(node.right, sj);
    }

    public String preorder() {
        StringJoiner sj = new StringJoiner(", ");
        preorder(root, sj);
        return sj.toString();
    }

    private void preorder(BSTNode node, StringJoiner sj) {
        if (node == null) return;
        sj.add(String.valueOf(node.value));
        preorder(node.left, sj);
        preorder(node.right, sj);
    }

    public String postorder() {
        StringJoiner sj = new StringJoiner(", ");
        postorder(root, sj);
        return sj.toString();
    }

    private void postorder(BSTNode node, StringJoiner sj) {
        if (node == null) return;
        postorder(node.left, sj);
        postorder(node.right, sj);
        sj.add(String.valueOf(node.value));
    }
}

public class BSTExample {

    public static void main(String[] args) {
        BST bst = new BST();
        bst.insert(50);
        bst.insert(30);
        bst.insert(70);
        bst.insert(20);
        bst.insert(40);
        bst.insert(60);
        bst.insert(80);

        System.out.println("Inorder: " + bst.inorder());
        System.out.println("Preorder: " + bst.preorder());
        System.out.println("Postorder: " + bst.postorder());
        System.out.println("Min: " + bst.findMin());
        System.out.println("Max: " + bst.findMax());
        System.out.println("Search 40: " + bst.search(40));
        System.out.println("Search 99: " + bst.search(99));

        bst.delete(50);
        System.out.println("Inorder after deleting 50: " + bst.inorder());
    }
}
