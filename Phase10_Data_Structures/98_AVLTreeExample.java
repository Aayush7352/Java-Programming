package phase10.datastructures;

class AVLTreeExample {

    static class Node {
        int key, height;
        Node left, right;

        Node(int key) {
            this.key = key;
            this.height = 1;
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
        } else {
            return node;
        }

        updateHeight(node);
        return balance(node);
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
        updateHeight(node);
        return balance(node);
    }

    private Node minValue(Node node) {
        while (node.left != null) node = node.left;
        return node;
    }

    private void updateHeight(Node node) {
        node.height = 1 + Math.max(height(node.left), height(node.right));
    }

    private int height(Node node) {
        return node == null ? 0 : node.height;
    }

    private int balanceFactor(Node node) {
        return node == null ? 0 : height(node.left) - height(node.right);
    }

    private Node balance(Node node) {
        var bf = balanceFactor(node);

        // LL case
        if (bf > 1 && balanceFactor(node.left) >= 0) {
            return rotateRight(node);
        }
        // LR case
        if (bf > 1 && balanceFactor(node.left) < 0) {
            node.left = rotateLeft(node.left);
            return rotateRight(node);
        }
        // RR case
        if (bf < -1 && balanceFactor(node.right) <= 0) {
            return rotateLeft(node);
        }
        // RL case
        if (bf < -1 && balanceFactor(node.right) > 0) {
            node.right = rotateRight(node.right);
            return rotateLeft(node);
        }
        return node;
    }

    private Node rotateRight(Node y) {
        var x = y.left;
        var T2 = x.right;
        x.right = y;
        y.left = T2;
        updateHeight(y);
        updateHeight(x);
        return x;
    }

    private Node rotateLeft(Node x) {
        var y = x.right;
        var T2 = y.left;
        y.left = x;
        x.right = T2;
        updateHeight(x);
        updateHeight(y);
        return y;
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

    public void printBalanceFactors() {
        System.out.print("Balance factors (inorder): ");
        printBFRec(root);
        System.out.println();
    }

    private void printBFRec(Node node) {
        if (node == null) return;
        printBFRec(node.left);
        System.out.print(node.key + ":" + balanceFactor(node) + " ");
        printBFRec(node.right);
    }

    public static void main(String[] args) {
        var avl = new AVLTreeExample();

        System.out.println("=== AVL Tree Demo ===");

        // LL case
        avl.insert(30);
        avl.insert(20);
        avl.insert(10);
        System.out.println("After inserting 30, 20, 10 (LL rotation):");
        avl.inorder();
        avl.printBalanceFactors();

        // RR case
        avl.insert(40);
        avl.insert(50);
        System.out.println("After inserting 40, 50 (RR rotation):");
        avl.inorder();
        avl.printBalanceFactors();

        // LR case
        avl.insert(5);
        avl.insert(7);
        System.out.println("After inserting 5, 7 (LR rotation):");
        avl.inorder();
        avl.printBalanceFactors();

        // RL case
        avl.insert(45);
        avl.insert(47);
        System.out.println("After inserting 45, 47 (RL rotation):");
        avl.inorder();
        avl.printBalanceFactors();

        System.out.println("\nDelete 10:");
        avl.delete(10);
        avl.inorder();
        avl.printBalanceFactors();
    }
}
