package phase14.designpatterns;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// Builder Pattern: Builder (lombok-style), Director, step-by-step construction, fluent API

// Product class
class Computer {
    private final String cpu;
    private final String gpu;
    private final int ramGB;
    private final int storageGB;
    private final String storageType;
    private final String os;
    private final boolean bluetooth;
    private final boolean wifi;
    private final List<String> peripherals;

    private Computer(Builder builder) {
        this.cpu = builder.cpu;
        this.gpu = builder.gpu;
        this.ramGB = builder.ramGB;
        this.storageGB = builder.storageGB;
        this.storageType = builder.storageType;
        this.os = builder.os;
        this.bluetooth = builder.bluetooth;
        this.wifi = builder.wifi;
        this.peripherals = List.copyOf(builder.peripherals);
    }

    @Override
    public String toString() {
        return "Computer{" +
                "cpu='" + cpu + '\'' +
                ", gpu='" + gpu + '\'' +
                ", ramGB=" + ramGB +
                ", storageGB=" + storageGB +
                ", storageType='" + storageType + '\'' +
                ", os='" + os + '\'' +
                ", bluetooth=" + bluetooth +
                ", wifi=" + wifi +
                ", peripherals=" + peripherals +
                '}';
    }

    // Lombok-style builder: static method builder(), inner Builder class
    public static Builder builder() {
        return new Builder();
    }

    // Fluent Builder (lombok-style @Builder)
    public static class Builder {
        private String cpu = "Intel i5";
        private String gpu = "Integrated";
        private int ramGB = 8;
        private int storageGB = 256;
        private String storageType = "SSD";
        private String os = "None";
        private boolean bluetooth = false;
        private boolean wifi = true;
        private final List<String> peripherals = new ArrayList<>();

        public Builder cpu(String cpu) { this.cpu = cpu; return this; }
        public Builder gpu(String gpu) { this.gpu = gpu; return this; }
        public Builder ramGB(int ramGB) { this.ramGB = ramGB; return this; }
        public Builder storageGB(int storageGB) { this.storageGB = storageGB; return this; }
        public Builder storageType(String storageType) { this.storageType = storageType; return this; }
        public Builder os(String os) { this.os = os; return this; }
        public Builder bluetooth(boolean bluetooth) { this.bluetooth = bluetooth; return this; }
        public Builder wifi(boolean wifi) { this.wifi = wifi; return this; }
        public Builder addPeripheral(String peripheral) { this.peripherals.add(peripheral); return this; }
        public Builder peripherals(List<String> peripherals) { this.peripherals.addAll(peripherals); return this; }

        // Consumer-based builder (Java 8+)
        public Builder configure(Consumer<Builder> config) {
            config.accept(this);
            return this;
        }

        public Computer build() {
            return new Computer(this);
        }
    }
}

// Director: orchestrates the building process for common configurations
class ComputerDirector {
    public static Computer buildGamingPC() {
        return Computer.builder()
                .cpu("Intel i9-13900K")
                .gpu("NVIDIA RTX 4090")
                .ramGB(64)
                .storageGB(2000)
                .storageType("NVMe SSD")
                .os("Windows 11 Pro")
                .bluetooth(true)
                .wifi(true)
                .addPeripheral("Mechanical Keyboard")
                .addPeripheral("Gaming Mouse")
                .addPeripheral("144Hz Monitor")
                .build();
    }

    public static Computer buildOfficePC() {
        return Computer.builder()
                .cpu("Intel i5-13400")
                .gpu("Integrated")
                .ramGB(16)
                .storageGB(512)
                .storageType("SSD")
                .os("Windows 11")
                .bluetooth(false)
                .wifi(true)
                .build();
    }

    public static Computer buildMacMini() {
        return Computer.builder()
                .cpu("Apple M2 Pro")
                .gpu("Integrated 19-core")
                .ramGB(32)
                .storageGB(1024)
                .storageType("SSD")
                .os("macOS Sonoma")
                .bluetooth(true)
                .wifi(true)
                .build();
    }

    public static Computer buildServer() {
        return Computer.builder()
                .cpu("AMD EPYC 9654")
                .gpu("None")
                .ramGB(512)
                .storageGB(40000)
                .storageType("NVMe RAID")
                .os("Ubuntu Server 22.04 LTS")
                .bluetooth(false)
                .wifi(false)
                .build();
    }
}

public class BuilderPattern {
    public static void main(String[] args) {
        System.out.println("=== Builder Pattern Demo ===\n");

        // 1. Basic builder usage (step-by-step)
        System.out.println("1. Step-by-step Builder (fluent API):");
        Computer basic = Computer.builder()
                .cpu("Intel i3")
                .ramGB(8)
                .storageGB(256)
                .os("Windows 11")
                .build();
        System.out.println("  " + basic);

        // 2. Builder with all options
        System.out.println("\n2. Full Builder (all options):");
        Computer custom = Computer.builder()
                .cpu("AMD Ryzen 9 7950X")
                .gpu("AMD Radeon RX 7900 XTX")
                .ramGB(128)
                .storageGB(4000)
                .storageType("NVMe SSD")
                .os("Arch Linux")
                .bluetooth(true)
                .wifi(true)
                .addPeripheral("Custom Keyboard")
                .addPeripheral("Trackball Mouse")
                .addPeripheral("Ultrawide Monitor")
                .build();
        System.out.println("  " + custom);

        // 3. Director: pre-defined configurations
        System.out.println("\n3. Director (pre-defined configurations):");
        System.out.println("  Gaming PC: " + ComputerDirector.buildGamingPC());
        System.out.println("  Office PC: " + ComputerDirector.buildOfficePC());
        System.out.println("  Mac Mini:  " + ComputerDirector.buildMacMini());
        System.out.println("  Server:    " + ComputerDirector.buildServer());

        // 4. Consumer-based builder (Java 8+ style)
        System.out.println("\n4. Consumer-based Builder:");
        Computer consumerBuilt = Computer.builder()
                .configure(b -> {
                    b.cpu("ARM Cortex-X4");
                    b.ramGB(16);
                    b.storageGB(128);
                    b.os("Android 14");
                    b.wifi(true);
                    b.bluetooth(true);
                })
                .build();
        System.out.println("  " + consumerBuilt);

        // 5. Builder with different configurations
        System.out.println("\n5. Multiple configurations with same builder:");
        var builder = Computer.builder()
                .os("Windows 11")
                .wifi(true);

        Computer home = builder.cpu("Intel i5").ramGB(16).storageGB(512).build();
        Computer work = builder.cpu("Intel i7").ramGB(32).storageGB(1000).build();

        System.out.println("  Home: " + home);
        System.out.println("  Work: " + work);

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("Builder pattern - separates object construction from its representation");
        System.out.println("Fluent API - method chaining (return this) for readable code");
        System.out.println("Director - pre-defined building sequences for common configurations");
        System.out.println("Step-by-step construction - build complex objects incrementally");
        System.out.println("Lombok-style @Builder - static builder() method + inner Builder class");
        System.out.println("Consumer-based builder - accepts lambda for configuration");
    }
}
