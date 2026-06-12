package phase14.designpatterns;

import java.util.ArrayDeque;
import java.util.Deque;

// Command Pattern: Command interface, concrete commands, invoker, receiver, undo support

// Receiver: knows how to perform the actual operations
class TextEditor {
    private final StringBuilder content = new StringBuilder();
    private int selectionStart = 0;
    private int selectionEnd = 0;

    public void insert(String text, int position) {
        content.insert(position, text);
        System.out.println("  [Receiver] Inserted '" + text + "' at position " + position);
    }

    public void delete(int start, int end) {
        content.delete(start, end);
        System.out.println("  [Receiver] Deleted from " + start + " to " + end);
    }

    public void replace(int start, int end, String replacement) {
        content.replace(start, end, replacement);
        System.out.println("  [Receiver] Replaced from " + start + " to " + end + " with '" + replacement + "'");
    }

    public String getContent() {
        return content.toString();
    }

    public void setSelection(int start, int end) {
        this.selectionStart = start;
        this.selectionEnd = end;
        System.out.println("  [Receiver] Selection set: " + start + " - " + end);
    }

    public int getSelectionStart() { return selectionStart; }
    public int getSelectionEnd() { return selectionEnd; }
}

// Command interface
@FunctionalInterface
interface Command {
    void execute();

    // Default method for undo support
    default void undo() {
        throw new UnsupportedOperationException("Undo not supported for this command");
    }
}

// Concrete commands
class InsertTextCommand implements Command {
    private final TextEditor editor;
    private final String text;
    private final int position;

    public InsertTextCommand(TextEditor editor, String text, int position) {
        this.editor = editor;
        this.text = text;
        this.position = position;
    }

    @Override
    public void execute() {
        editor.insert(text, position);
    }

    @Override
    public void undo() {
        editor.delete(position, position + text.length());
        System.out.println("  [Undo] Undone insert of '" + text + "'");
    }
}

class DeleteTextCommand implements Command {
    private final TextEditor editor;
    private final int start;
    private final int end;
    private String deletedText;

    public DeleteTextCommand(TextEditor editor, int start, int end) {
        this.editor = editor;
        this.start = start;
        this.end = end;
    }

    @Override
    public void execute() {
        // Store the deleted text for undo
        deletedText = editor.getContent().substring(start, end);
        editor.delete(start, end);
    }

    @Override
    public void undo() {
        editor.insert(deletedText, start);
        System.out.println("  [Undo] Restored deleted text '" + deletedText + "'");
    }
}

class ReplaceTextCommand implements Command {
    private final TextEditor editor;
    private final int start;
    private final int end;
    private final String replacement;
    private String originalText;

    public ReplaceTextCommand(TextEditor editor, int start, int end, String replacement) {
        this.editor = editor;
        this.start = start;
        this.end = end;
        this.replacement = replacement;
    }

    @Override
    public void execute() {
        originalText = editor.getContent().substring(start, end);
        editor.replace(start, end, replacement);
    }

    @Override
    public void undo() {
        editor.replace(start, start + replacement.length(), originalText);
        System.out.println("  [Undo] Restored original text '" + originalText + "'");
    }
}

// Macro command: composite of multiple commands
class MacroCommand implements Command {
    private final java.util.List<Command> commands = new java.util.ArrayList<>();

    public void addCommand(Command command) {
        commands.add(command);
    }

    @Override
    public void execute() {
        System.out.println("  [Macro] Executing " + commands.size() + " commands:");
        for (var cmd : commands) {
            cmd.execute();
        }
    }

    @Override
    public void undo() {
        System.out.println("  [Macro] Undoing " + commands.size() + " commands (reverse order):");
        var reversed = new java.util.ArrayList<>(commands);
        java.util.Collections.reverse(reversed);
        for (var cmd : reversed) {
            cmd.undo();
        }
    }
}

// Invoker
class CommandInvoker {
    private final Deque<Command> history = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    public void executeCommand(Command command) {
        System.out.println("\n  [Invoker] Executing command...");
        command.execute();
        history.push(command);
        redoStack.clear(); // Clear redo stack on new command
        printState();
    }

    public void undo() {
        if (history.isEmpty()) {
            System.out.println("\n  [Invoker] Nothing to undo");
            return;
        }
        System.out.println("\n  [Invoker] Undoing last command...");
        Command command = history.pop();
        command.undo();
        redoStack.push(command);
        printState();
    }

    public void redo() {
        if (redoStack.isEmpty()) {
            System.out.println("\n  [Invoker] Nothing to redo");
            return;
        }
        System.out.println("\n  [Invoker] Redoing command...");
        Command command = redoStack.pop();
        command.execute();
        history.push(command);
        printState();
    }

    public boolean canUndo() { return !history.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    private void printState() {
        System.out.println("  [State] \"" + editor.getContent() + "\"");
    }

    // Reference to editor for state display
    private static TextEditor editor;

    public static void setEditor(TextEditor e) { editor = e; }
}

// Lambda command demo (functional approach)
class LambdaCommand implements Command {
    private final Runnable executeAction;
    private final Runnable undoAction;

    public LambdaCommand(Runnable execute, Runnable undo) {
        this.executeAction = execute;
        this.undoAction = undo;
    }

    @Override
    public void execute() { executeAction.run(); }

    @Override
    public void undo() {
        if (undoAction != null) undoAction.run();
    }
}

// Another receiver example: Light
class Light {
    public void turnOn() { System.out.println("  [Light] Turned ON"); }
    public void turnOff() { System.out.println("  [Light] Turned OFF"); }
}

public class CommandPattern {
    public static void main(String[] args) {
        System.out.println("=== Command Pattern Demo ===\n");

        var editor = new TextEditor();
        CommandInvoker.setEditor(editor);
        var invoker = new CommandInvoker();

        // 1. Insert commands
        System.out.println("1. Insert Commands:");
        invoker.executeCommand(new InsertTextCommand(editor, "Hello", 0));
        invoker.executeCommand(new InsertTextCommand(editor, " World", 5));

        // 2. Undo
        System.out.println("\n2. Undo Operations:");
        invoker.undo();
        invoker.undo();

        // 3. Redo
        System.out.println("\n3. Redo Operations:");
        invoker.redo();
        invoker.redo();

        // 4. Delete command with undo
        System.out.println("\n4. Delete with Undo:");
        invoker.executeCommand(new InsertTextCommand(editor, "Java 21", 0));
        invoker.executeCommand(new DeleteTextCommand(editor, 4, 6)); // delete "21"
        invoker.undo();

        // 5. Replace command
        System.out.println("\n5. Replace with Undo:");
        invoker.executeCommand(new ReplaceTextCommand(editor, 0, 4, "Python"));

        // 6. Macro command (composite)
        System.out.println("\n6. Macro Command (batch operations):");
        var macro = new MacroCommand();
        macro.addCommand(new InsertTextCommand(editor, "The quick brown fox ", 0));
        macro.addCommand(new InsertTextCommand(editor, "jumps over ", 19));
        macro.addCommand(new InsertTextCommand(editor, "the lazy dog.", 30));
        invoker.executeCommand(macro);

        // Undo the macro
        invoker.undo();

        // 7. Lambda commands (functional)
        System.out.println("\n7. Lambda Commands (functional):");
        final String[] clipboard = {""};
        var copyCommand = new LambdaCommand(
                () -> {
                    clipboard[0] = editor.getContent();
                    System.out.println("  [Lambda] Copied content: \"" + clipboard[0] + "\"");
                },
                () -> System.out.println("  [Lambda] Copy undo: clipboard cleared")
        );
        invoker.executeCommand(copyCommand);

        // 8. Different receiver: Light
        System.out.println("\n8. Command Pattern with different Receiver (Light):");
        var light = new Light();

        // Using lambda commands
        var lightOn = new LambdaCommand(light::turnOn, light::turnOff);
        var lightOff = new LambdaCommand(light::turnOff, light::turnOn);

        var lightInvoker = new CommandInvoker();
        lightInvoker.executeCommand(lightOn);
        lightInvoker.executeCommand(lightOff);
        lightInvoker.undo();

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("Command interface - declares execute() and default undo()");
        System.out.println("Concrete commands - implement specific actions (Insert, Delete, Replace)");
        System.out.println("Invoker - stores and executes commands, manages history");
        System.out.println("Receiver - knows how to perform the actual operations (TextEditor, Light)");
        System.out.println("Undo support - each command stores its inverse operation");
        System.out.println("Macro command - composite command that executes multiple sub-commands");
        System.out.println("Lambda commands - functional approach using Runnable or custom functional interfaces");
    }
}
