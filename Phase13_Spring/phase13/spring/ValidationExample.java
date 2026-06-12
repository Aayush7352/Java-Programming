package phase13.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// --- Demo: @Valid, @NotNull, @Size, @Email, @Pattern, @Validated, custom validator (Jakarta Validation concepts) ---

// Jakarta Validation annotation mimics
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface NotNull {
    String message() default "must not be null";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface Size {
    int min() default 0;
    int max() default Integer.MAX_VALUE;
    String message() default "size out of bounds";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface Email {
    String message() default "must be a valid email";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface PatternAnnot {
    String regexp();
    String message() default "must match pattern";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Validated {}

// Custom validator annotation
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface AgeRange {
    int min() default 0;
    int max() default 150;
    String message() default "age out of range";
}

// Simple validation error
record ValidationError(String field, String message) {}

// Validation context
class ValidationContext {
    private final List<ValidationError> errors = new ArrayList<>();

    public void addError(String field, String message) {
        errors.add(new ValidationError(field, message));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<ValidationError> getErrors() {
        return List.copyOf(errors);
    }

    public void printErrors() {
        if (errors.isEmpty()) {
            System.out.println("  [Validation] All checks passed!");
        } else {
            errors.forEach(e -> System.out.println("  [Validation Error] " + e.field() + ": " + e.message()));
        }
    }
}

// Simple validator engine
class ValidatorEngine {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public void validate(Object obj, ValidationContext context) {
        var fields = obj.getClass().getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
            Object value;
            try {
                value = field.get(obj);
            } catch (IllegalAccessException e) {
                continue;
            }

            // @NotNull
            if (field.isAnnotationPresent(NotNull.class) && value == null) {
                context.addError(field.getName(), field.getAnnotation(NotNull.class).message());
            }

            // @Size
            if (field.isAnnotationPresent(Size.class) && value instanceof String s) {
                var size = field.getAnnotation(Size.class);
                if (s.length() < size.min() || s.length() > size.max()) {
                    context.addError(field.getName(), size.message()
                            .replace("size out of bounds",
                            "must be between " + size.min() + " and " + size.max()));
                }
            }

            // @Email
            if (field.isAnnotationPresent(Email.class) && value instanceof String s) {
                if (!EMAIL_PATTERN.matcher(s).matches()) {
                    context.addError(field.getName(), field.getAnnotation(Email.class).message());
                }
            }

            // @PatternAnnot
            if (field.isAnnotationPresent(PatternAnnot.class) && value instanceof String s) {
                var p = field.getAnnotation(PatternAnnot.class);
                if (!Pattern.compile(p.regexp()).matcher(s).matches()) {
                    context.addError(field.getName(), p.message());
                }
            }

            // @AgeRange (custom validator)
            if (field.isAnnotationPresent(AgeRange.class) && value instanceof Integer age) {
                var range = field.getAnnotation(AgeRange.class);
                if (age < range.min() || age > range.max()) {
                    context.addError(field.getName(), range.message()
                            .replace("age out of range",
                            "must be between " + range.min() + " and " + range.max()));
                }
            }
        }
    }
}

// A DTO/Model class using validation annotations
@Validated
class UserRegistration {
    @NotNull(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotNull(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotNull(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @PatternAnnot(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).+$",
            message = "Password must contain at least one digit, one lowercase and one uppercase letter")
    private String password;

    @AgeRange(min = 18, max = 120, message = "Age must be between 18 and 120")
    private int age;

    public UserRegistration(String username, String email, String password, int age) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.age = age;
    }
}

public class ValidationExample {
    public static void main(String[] args) {
        System.out.println("=== Jakarta Validation Demo ===");

        var validator = new ValidatorEngine();

        // --- Test 1: Valid user ---
        System.out.println("\n1. Valid User:");
        var validUser = new UserRegistration("john_doe", "john@example.com", "Password123", 25);
        var ctx1 = new ValidationContext();
        validator.validate(validUser, ctx1);
        ctx1.printErrors();

        // --- Test 2: Invalid user (violating all constraints) ---
        System.out.println("\n2. Invalid User (null username, bad email, weak password, underage):");
        var invalidUser = new UserRegistration(null, "not-an-email", "weak", 16);
        var ctx2 = new ValidationContext();
        validator.validate(invalidUser, ctx2);
        ctx2.printErrors();

        // --- Test 3: Boundary cases ---
        System.out.println("\n3. Boundary Cases:");
        var boundaryUser = new UserRegistration("ab", "test@", "Short1A", 17);
        var ctx3 = new ValidationContext();
        validator.validate(boundaryUser, ctx3);
        ctx3.printErrors();

        // --- Test 4: Custom validator (@AgeRange) ---
        System.out.println("\n4. Custom @AgeRange Validator:");
        var oldUser = new UserRegistration("oldman", "old@example.com", "OldMan123", 200);
        var ctx4 = new ValidationContext();
        validator.validate(oldUser, ctx4);
        ctx4.printErrors();

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("@Valid - triggers validation on a nested object");
        System.out.println("@NotNull - validates that a value is not null");
        System.out.println("@Size(min, max) - validates string/collection size");
        System.out.println("@Email - validates email format");
        System.out.println("@Pattern(regexp) - validates against a regex");
        System.out.println("@Validated - class-level validation trigger");
        System.out.println("Custom validator (@AgeRange) - user-defined validation logic");
        System.out.println("Jakarta Validation (formerly JSR-380 / Bean Validation 2.0)");
    }
}
