package phase10.datastructures;

class TrieExample {

    static class TrieNode {
        TrieNode[] children = new TrieNode[26];
        boolean isEnd;
    }

    private final TrieNode root = new TrieNode();

    public void insert(String word) {
        var node = root;
        for (char ch : word.toCharArray()) {
            int idx = ch - 'a';
            if (idx < 0 || idx >= 26) {
                throw new IllegalArgumentException("Only lowercase letters allowed");
            }
            if (node.children[idx] == null) {
                node.children[idx] = new TrieNode();
            }
            node = node.children[idx];
        }
        node.isEnd = true;
    }

    public boolean search(String word) {
        var node = findNode(word);
        return node != null && node.isEnd;
    }

    public boolean startsWith(String prefix) {
        return findNode(prefix) != null;
    }

    public void delete(String word) {
        deleteRec(root, word, 0);
    }

    private boolean deleteRec(TrieNode node, String word, int depth) {
        if (node == null) return false;
        if (depth == word.length()) {
            if (!node.isEnd) return false;
            node.isEnd = false;
            return hasNoChildren(node);
        }
        int idx = word.charAt(depth) - 'a';
        boolean shouldDeleteChild = deleteRec(node.children[idx], word, depth + 1);
        if (shouldDeleteChild) {
            node.children[idx] = null;
            return !node.isEnd && hasNoChildren(node);
        }
        return false;
    }

    private boolean hasNoChildren(TrieNode node) {
        for (var child : node.children) {
            if (child != null) return false;
        }
        return true;
    }

    private TrieNode findNode(String prefix) {
        var node = root;
        for (char ch : prefix.toCharArray()) {
            int idx = ch - 'a';
            if (idx < 0 || idx >= 26 || node.children[idx] == null) return null;
            node = node.children[idx];
        }
        return node;
    }

    public static void main(String[] args) {
        var trie = new TrieExample();

        trie.insert("hello");
        trie.insert("world");
        trie.insert("hi");
        trie.insert("high");
        trie.insert("help");

        System.out.println("Search 'hello': " + trie.search("hello"));
        System.out.println("Search 'world': " + trie.search("world"));
        System.out.println("Search 'hey': " + trie.search("hey"));
        System.out.println("Search 'hi': " + trie.search("hi"));

        System.out.println("StartsWith 'he': " + trie.startsWith("he"));
        System.out.println("StartsWith 'wo': " + trie.startsWith("wo"));
        System.out.println("StartsWith 'xyz': " + trie.startsWith("xyz"));

        trie.delete("hi");
        System.out.println("After deleting 'hi':");
        System.out.println("Search 'hi': " + trie.search("hi"));
        System.out.println("Search 'high': " + trie.search("high"));

        trie.delete("hello");
        System.out.println("After deleting 'hello':");
        System.out.println("Search 'hello': " + trie.search("hello"));
        System.out.println("Search 'help': " + trie.search("help"));
    }
}
