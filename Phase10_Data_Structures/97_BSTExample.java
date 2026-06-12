package phase10.datastructures;

class BSTExample {

    static class Node {
        int key;
        Node left, right;

        Node(int key) {
            this.key = key;
        }
    }

    private Node root;

    public void insert(int key) {
        root = insertRec(root, key);
    }

    private Node insertRec(Node node, int key) {
        if (node == null) return new Node(key);
        if (key < node.key) {
            node.left = insertRec(node.left, key);
        } else if (key > node.key) {
            node.right = insertRec(node.right, key);
        }
        return node;
    }

    public boolean search(int key) {
        return searchRec(root, key);
    }

    private boolean searchRec(Node node, int key) {
        if (node == null) return false;
        if (key == node.key) return true;
        return key < node.key
            ? searchRec(node.left, key)
            : searchRec(node.right, key);
    }

    public void delete(int key) {
        root = deleteRec(root, key);
    }

    private Node deleteRec(Node node, int key) {
        if (node == null) return null;
        if (key < node.key) {
            node.left = deleteRec(node.left, key);
        } else if (key > node.key) {
            node.right = deleteRec(node.right, key);
        } else {
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;
            var successor = minValue(node.right);
            node.key = successor.key;
            node.right = deleteRec(node.right, successor.key);
        }
        return node;
    }

    private Node minValue(Node node) {
        while (node.left != null) node = node.left;
        return node;
    }

    public void inorder() {
        System.out.print("Inorder: ");
        inorderRec(root);
        System.out.println();
    }

    private void inorderRec(Node node) {
        if (node == null) return;
        inorderRec(node.left);
        System.out.print(node.key + " ");
        inorderRec(node.right);
    }

    public void preorder() {
        System.out.print("Preorder: ");
        preorderRec(root);
        System.out.println();
    }

    private void preorderRec(Node node) {
        if (node == null) return;
        System.out.print(node.key + " ");
        preorderRec(node.left);
        preorderRec(node.right);
    }

    public void postorder() {
        System.out.print("Postorder: ");
        postorderRec(root);
        System.out.println();
    }

    private void postorderRec(Node node) {
        if (node == null) return;
        postorderRec(node.left);
        postorderRec(node.right);
        System.out.print(node.key + " ");
    }

    public static void main(String[] args) {
        var bst = new BSTExample();

        for (int key : new int[]{50, 30, 70, 20, 40, 60, 80, 10, 25}) {
            bst.insert(key);
        }

        bst.inorder();
        bst.preorder();
        bst.postorder();

        System.out.println("Search 40: " + bst.search(40));
        System.out.println("Search 100: " + bst.search(100));

        bst.delete(20);
        System.out.println("After deleting 20:");
        bst.inorder();

        bst.delete(30);
        System.out.println("After deleting 30:");
        bst.inorder();

        bst.delete(50);
        System.out.println("After deleting 50:");
        bst.inorder();
    }
}
