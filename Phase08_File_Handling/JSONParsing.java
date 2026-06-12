package phase08.filehandling;

import java.util.*;

/**
 * Minimal JSON parser — no external libraries required.
 * Supports strings, numbers, booleans, null, objects, and arrays.
 */
class JsonValue {
    enum Type { STRING, NUMBER, BOOLEAN, NULL, OBJECT, ARRAY }

    private final Type type;
    private final Object value;

    private JsonValue(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    static JsonValue ofString(String s)    { return new JsonValue(Type.STRING, s); }
    static JsonValue ofNumber(double n)    { return new JsonValue(Type.NUMBER, n); }
    static JsonValue ofBoolean(boolean b)  { return new JsonValue(Type.BOOLEAN, b); }
    static JsonValue ofNull()              { return new JsonValue(Type.NULL, null); }
    static JsonValue ofObject(Map<String, JsonValue> m) { return new JsonValue(Type.OBJECT, m); }
    static JsonValue ofArray(List<JsonValue> a)         { return new JsonValue(Type.ARRAY, a); }

    Type type() { return type; }

    String asString() {
        if (type != Type.STRING) throw new IllegalStateException("Not a string: " + type);
        return (String) value;
    }
    double asNumber() {
        if (type != Type.NUMBER) throw new IllegalStateException("Not a number: " + type);
        return (Double) value;
    }
    boolean asBoolean() {
        if (type != Type.BOOLEAN) throw new IllegalStateException("Not a boolean: " + type);
        return (Boolean) value;
    }
    Map<String, JsonValue> asObject() {
        if (type != Type.OBJECT) throw new IllegalStateException("Not an object: " + type);
        @SuppressWarnings("unchecked")
        var m = (Map<String, JsonValue>) value;
        return m;
    }
    List<JsonValue> asArray() {
        if (type != Type.ARRAY) throw new IllegalStateException("Not an array: " + type);
        @SuppressWarnings("unchecked")
        var a = (List<JsonValue>) value;
        return a;
    }

    @Override
    public String toString() {
        return switch (type) {
            case STRING -> "\"" + escape((String) value) + "\"";
            case NUMBER -> value.toString();
            case BOOLEAN -> value.toString();
            case NULL -> "null";
            case OBJECT -> {
                var sb = new StringBuilder("{");
                var map = asObject();
                var first = true;
                for (var e : map.entrySet()) {
                    if (!first) sb.append(",");
                    sb.append("\"").append(escape(e.getKey())).append("\":").append(e.getValue());
                    first = false;
                }
                sb.append("}");
                yield sb.toString();
            }
            case ARRAY -> {
                var sb = new StringBuilder("[");
                var first = true;
                for (var v : asArray()) {
                    if (!first) sb.append(",");
                    sb.append(v);
                    first = false;
                }
                sb.append("]");
                yield sb.toString();
            }
        };
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}

class JsonParser {
    private final String input;
    private int pos;

    JsonParser(String input) { this.input = input; pos = 0; }

    JsonValue parse() {
        skipWhitespace();
        if (pos >= input.length()) throw new RuntimeException("Unexpected end");
        return switch (input.charAt(pos)) {
            case '{' -> JsonValue.ofObject(parseObject());
            case '[' -> JsonValue.ofArray(parseArray());
            case '"' -> JsonValue.ofString(parseString());
            case 't', 'f' -> JsonValue.ofBoolean(parseBoolean());
            case 'n' -> { parseNull(); yield JsonValue.ofNull(); }
            default -> JsonValue.ofNumber(parseNumber());
        };
    }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++;
    }

    private Map<String, JsonValue> parseObject() {
        expect('{');
        var map = new LinkedHashMap<String, JsonValue>();
        skipWhitespace();
        if (input.charAt(pos) == '}') { pos++; return map; }
        while (true) {
            skipWhitespace();
            var key = parseString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            map.put(key, parse());
            skipWhitespace();
            if (input.charAt(pos) == '}') { pos++; return map; }
            expect(',');
        }
    }

    private List<JsonValue> parseArray() {
        expect('[');
        var list = new ArrayList<JsonValue>();
        skipWhitespace();
        if (input.charAt(pos) == ']') { pos++; return list; }
        while (true) {
            skipWhitespace();
            list.add(parse());
            skipWhitespace();
            if (input.charAt(pos) == ']') { pos++; return list; }
            expect(',');
        }
    }

    private String parseString() {
        expect('"');
        var sb = new StringBuilder();
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '"') { pos++; return sb.toString(); }
            if (c == '\\') {
                pos++;
                if (pos >= input.length()) throw new RuntimeException("Unexpected end in string escape");
                char next = input.charAt(pos);
                sb.append(switch (next) {
                    case '"', '\\', '/' -> next;
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case 'u' -> {
                        if (pos + 4 >= input.length()) throw new RuntimeException("Invalid unicode escape");
                        int code = Integer.parseInt(input.substring(pos + 1, pos + 5), 16);
                        pos += 4;
                        yield (char) code;
                    }
                    default -> throw new RuntimeException("Invalid escape: \\" + next);
                });
                pos++;
            } else {
                sb.append(c);
                pos++;
            }
        }
        throw new RuntimeException("Unterminated string");
    }

    private double parseNumber() {
        int start = pos;
        if (input.charAt(pos) == '-') pos++;
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
        if (pos < input.length() && input.charAt(pos) == '.') {
            pos++;
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
        }
        if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
            pos++;
            if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) pos++;
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
        }
        return Double.parseDouble(input.substring(start, pos));
    }

    private boolean parseBoolean() {
        if (input.startsWith("true", pos)) { pos += 4; return true; }
        if (input.startsWith("false", pos)) { pos += 5; return false; }
        throw new RuntimeException("Expected boolean at " + pos);
    }

    private void parseNull() {
        if (input.startsWith("null", pos)) { pos += 4; return; }
        throw new RuntimeException("Expected null at " + pos);
    }

    private void expect(char c) {
        skipWhitespace();
        if (pos >= input.length() || input.charAt(pos) != c)
            throw new RuntimeException("Expected '" + c + "' at " + pos);
        pos++;
    }
}

public class JSONParsing {
    public static void main(String[] args) {
        // Sample JSON string
        String json = """
                {
                    "name": "Alice",
                    "age": 30,
                    "active": true,
                    "address": {
                        "city": "New York",
                        "zip": "10001"
                    },
                    "phones": ["555-0100", "555-0200"],
                    "metadata": null
                }
                """;

        System.out.println("=== Parsing JSON with minimal parser ===\n");

        var parser = new JsonParser(json);
        var value = parser.parse();

        System.out.println("Pretty-printed JSON:");
        System.out.println(value);
        System.out.println();

        // Access parsed data
        var obj = value.asObject();
        System.out.println("name: " + obj.get("name").asString());
        System.out.println("age: " + obj.get("age").asNumber());
        System.out.println("active: " + obj.get("active").asBoolean());
        System.out.println("metadata: " + obj.get("metadata").type());

        var address = obj.get("address").asObject();
        System.out.println("city: " + address.get("city").asString());

        var phones = obj.get("phones").asArray();
        System.out.println("phones: " + phones);

        // JSON array parsing
        String jsonArray = """
                [10, 20, 30, 40, 50]
                """;
        var arrParser = new JsonParser(jsonArray);
        var arrValue = arrParser.parse();
        System.out.println("\nParsed array: " + arrValue);
        System.out.println("Element at [2]: " + arrValue.asArray().get(2).asNumber());

        // Round-trip: serialize back
        System.out.println("\n=== Round-trip test ===");
        var roundTrip = new JsonParser(value.toString()).parse();
        System.out.println("Re-parsed: " + roundTrip);
        System.out.println("Match: " + value.toString().equals(roundTrip.toString()));
    }
}
