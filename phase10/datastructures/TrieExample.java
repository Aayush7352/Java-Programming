package phase10.datastructures;

import java.util.ArrayList;
import java.util.List;

class TrieNode {
    TrieNode[] children = new TrieNode[26];
    boolean isEnd;
}

class Trie {
    private final TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    public void insert(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int idx = c - 'a';
            if (node.children[idx] == null) node.children[idx] = new TrieNode();
            node = node.children[idx];
        }
        node.isEnd = true;
    }

    public boolean search(String word) {
        TrieNode node = find(word);
        return node != null && node.isEnd;
    }

    public boolean startsWith(String prefix) {
        return find(prefix) != null;
    }

    public boolean delete(String word) {
        return delete(root, word, 0);
    }

    private boolean delete(TrieNode node, String word, int depth) {
        if (node == null) return false;
        if (depth == word.length()) {
            if (!node.isEnd) return false;
            node.isEnd = false;
            return hasNoChildren(node);
        }
        int idx = word.charAt(depth) - 'a';
        if (delete(node.children[idx], word, depth + 1)) {
            node.children[idx] = null;
            return !node.isEnd && hasNoChildren(node);
        }
        return false;
    }

    private boolean hasNoChildren(TrieNode node) {
        for (TrieNode child : node.children) if (child != null) return false;
        return true;
    }

    private TrieNode find(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int idx = c - 'a';
            if (node.children[idx] == null) return null;
            node = node.children[idx];
        }
        return node;
    }

    public List<String> autocomplete(String prefix) {
        List<String> result = new ArrayList<>();
        TrieNode node = find(prefix);
        if (node == null) return result;
        dfs(node, new StringBuilder(prefix), result);
        return result;
    }

    private void dfs(TrieNode node, StringBuilder sb, List<String> result) {
        if (node.isEnd) result.add(sb.toString());
        for (int i = 0; i < 26; i++) {
            if (node.children[i] != null) {
                sb.append((char) ('a' + i));
                dfs(node.children[i], sb, result);
                sb.deleteCharAt(sb.length() - 1);
            }
        }
    }
}

public class TrieExample {

    public static void main(String[] args) {
        Trie trie = new Trie();
        trie.insert("apple");
        trie.insert("app");
        trie.insert("apricot");
        trie.insert("banana");
        trie.insert("bat");

        System.out.println("Search 'apple': " + trie.search("apple"));
        System.out.println("Search 'app': " + trie.search("app"));
        System.out.println("Search 'apt': " + trie.search("apt"));
        System.out.println("startsWith 'ap': " + trie.startsWith("ap"));
        System.out.println("startsWith 'ba': " + trie.startsWith("ba"));

        System.out.println("Autocomplete 'ap': " + trie.autocomplete("ap"));

        trie.delete("app");
        System.out.println("After deleting 'app'");
        System.out.println("Search 'app': " + trie.search("app"));
        System.out.println("Autocomplete 'ap': " + trie.autocomplete("ap"));
    }
}
