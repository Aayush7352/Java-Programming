package phase13.spring;

import java.util.HashMap;
import java.util.Map;

// --- Demo: IoC Container, BeanFactory, ApplicationContext, XML config concept, annotations ---

// Mimics a simple IoC container
interface BeanFactory {
    Object getBean(String name);
}

// Mimics ApplicationContext (extends BeanFactory with additional features)
interface ApplicationContext extends BeanFactory {
    void refresh();
    void registerBean(String name, Object bean);
}

// Simple annotation mimics
@interface Component {
    String value() default "";
}

@interface Autowired {}

// A sample bean
@Component("greetingService")
class GreetingService {
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}

// Another bean that uses Autowired
@Component("messageProcessor")
class MessageProcessor {
    private GreetingService greetingService;

    @Autowired
    public void setGreetingService(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    public String process(String name) {
        return greetingService.greet(name);
    }
}

// Simple ApplicationContext implementation mimicking XML + annotation-based config
class SimpleApplicationContext implements ApplicationContext {
    private final Map<String, Object> beans = new HashMap<>();

    @Override
    public void registerBean(String name, Object bean) {
        beans.put(name, bean);
    }

    @Override
    public Object getBean(String name) {
        Object bean = beans.get(name);
        if (bean == null) {
            throw new RuntimeException("No bean named '" + name + "' found");
        }
        return bean;
    }

    @Override
    public void refresh() {
        // In real Spring, this would scan packages for @Component, resolve @Autowired, etc.
        System.out.println("[IoC Container] Refreshing ApplicationContext...");
        if (beans.containsKey("greetingService") && beans.containsKey("messageProcessor")) {
            var processor = (MessageProcessor) beans.get("messageProcessor");
            var service = (GreetingService) beans.get("greetingService");
            // Mimicking @Autowired injection
            processor.setGreetingService(service);
        }
        System.out.println("[IoC Container] All beans initialized and wired.");
    }
}

public class SpringCoreExample {
    public static void main(String[] args) {
        System.out.println("=== Spring Core: IoC Container Demo ===");

        // 1. BeanFactory concept
        BeanFactory factory = new SimpleApplicationContext();
        System.out.println("[BeanFactory] Created.");

        // 2. ApplicationContext concept (with XML/annotation config simulation)
        SimpleApplicationContext context = new SimpleApplicationContext();
        context.registerBean("greetingService", new GreetingService());
        context.registerBean("messageProcessor", new MessageProcessor());
        context.refresh();

        // 3. Retrieve and use beans
        GreetingService service = (GreetingService) context.getBean("greetingService");
        System.out.println("[Bean] " + service.greet("Spring"));

        MessageProcessor processor = (MessageProcessor) context.getBean("messageProcessor");
        System.out.println("[Bean with @Autowired] " + processor.process("Spring IoC"));

        // Concept summary
        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("BeanFactory: getBean() - basic IoC container");
        System.out.println("ApplicationContext: refresh(), registerBean() - advanced container with DI");
        System.out.println("@Component(\"beanName\") - marks a class as a Spring-managed bean");
        System.out.println("@Autowired - injects dependencies automatically");
        System.out.println("XML config concept: beans defined externally, wired by container");
    }
}
