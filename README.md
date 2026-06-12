# Java Mastery: 156 Programs — 16 Phases

[![Java](https://img.shields.io/badge/Java-21%2B-orange)](https://openjdk.org/projects/jdk/21/)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)](CONTRIBUTING.md)

A comprehensive, production-quality collection of **156 Java programs** organized across **16 progressive phases** — from fundamental syntax to building a production-grade microservices platform. Every program compiles with **Java 21**, leverages modern language features (records, sealed classes, pattern matching, virtual threads), and includes a `main()` method for immediate execution.

---

##  Why This Repository

| Goal | Approach |
|------|----------|
| **Learn Java deeply** | Each topic is a self-contained program with focused demonstrations |
| **Bridge theory & practice** | Phase 16 contains 12 real-world projects (300–580 lines each) |
| **Stay modern** | Uses Java 21 features: text blocks, switch expressions, records, sealed classes, pattern matching, virtual threads |
| **Interview-ready** | Covers DSA, algorithms, OOP design, concurrency, Spring, microservices, and system design |

---

##  Phase Map

### Phase 1 — Basics
| # | File | Topic |
|---|------|-------|
| 1 | `01_HelloWorld.java` | Hello World |
| 2 | `02_VariablesAndDataTypes.java` | Primitives, `var`, text blocks |
| 3 | `03_Operators.java` | All operator types |
| 4 | `04_InputOutput.java` | Scanner, printf, Console |
| 5 | `05_TypeCasting.java` | Implicit, explicit, wrapper conversions |
| 6 | `06_ConditionalStatements.java` | if/else, switch expressions, pattern matching |
| 7 | `07_Loops.java` | for, while, for-each, labeled break/continue |
| 8 | `08_Arrays.java` | 1D, 2D, jagged, Arrays utility |
| 9 | `09_Strings.java` | String, StringBuilder, StringJoiner, text blocks |
| 10 | `10_Methods.java` | Overloading, varargs, pass-by-value |
| 11 | `11_Recursion.java` | Factorial, Fibonacci, Tower of Hanoi |
| 12 | `12_PatternPrograms.java` | 12 star & number patterns |
| 13 | `13_NumberPrograms.java` | Prime, palindrome, Armstrong, GCD, LCM |
| 14 | `14_Searching.java` | Linear & binary search |
| 15 | `15_Sorting.java` | Bubble → Quick sort, `parallelSort` |

### Phase 2 — OOP
| # | File | Topic |
|---|------|-------|
| 16 | `16_ClassesAndObjects.java` | Class design, access modifiers |
| 17 | `17_Constructors.java` | Default, parameterized, copy, chaining |
| 18 | `18_ThisKeyword.java` | `this` for shadowing, chaining, args |
| 19 | `19_StaticKeyword.java` | Static fields, methods, blocks, imports |
| 20 | `20_Encapsulation.java` | Getters/setters, validation |
| 21 | `21_Inheritance.java` | `extends`, `super`, multi-level |
| 22 | `22_MethodOverriding.java` | `@Override`, covariant returns |
| 23 | `23_Polymorphism.java` | Overloading, overriding, pattern matching |
| 24 | `24_Abstraction.java` | Abstract classes vs interfaces |
| 25 | `25_Interfaces.java` | Default, static, private methods |
| 26 | `26_AbstractClasses.java` | Abstract class with constructor |
| 27 | `27_Composition.java` | Strong "has-a" relationship |
| 28 | `28_Association.java` | Bidirectional & unidirectional |
| 29 | `29_Aggregation.java` | Weak "has-a" relationship |
| 30 | `30_SOLIDPrinciples.java` | All 5 SOLID principles |

### Phase 3 — Advanced OOP
| # | File | Topic |
|---|------|-------|
| 31 | `31_ObjectClass.java` | `toString`, `equals`, `hashCode` |
| 32 | `32_EqualsAndHashCode.java` | Contract + records comparison |
| 33 | `33_Comparable.java` | Natural ordering with `compareTo` |
| 34 | `34_Comparator.java` | `Comparator.comparing`, `thenComparing` |
| 35 | `35_Cloneable.java` | Shallow vs deep copy |
| 36 | `36_Records.java` | Compact constructor, validation |
| 37 | `37_SealedClasses.java` | Sealed hierarchy, exhaustive switch |
| 38 | `38_NestedClasses.java` | Static nested, method-local inner |
| 39 | `39_InnerClasses.java` | Member inner class |
| 40 | `40_AnonymousClasses.java` | Anonymous class patterns |

### Phase 4 — Exception Handling
| # | File | Topic |
|---|------|-------|
| 41 | `41_TryCatch.java` | try-catch, exception methods |
| 42 | `42_MultipleCatch.java` | Multi-catch, specificity |
| 43 | `43_Finally.java` | try-catch-finally guarantees |
| 44 | `44_Throw.java` | `throw` checked/unchecked |
| 45 | `45_Throws.java` | `throws` propagation |
| 46 | `46_CustomExceptions.java` | Custom checked & unchecked |
| 47 | `47_ExceptionHierarchy.java` | Throwable hierarchy |
| 48 | `48_BestPractices.java` | Try-with-resources, chaining |

### Phase 5 — Collections Framework
| # | File | Topic |
|---|------|-------|
| 49 | `ArrayListExample.java` | CRUD, ListIterator, forEach |
| 50 | `LinkedListExample.java` | List + Deque operations |
| 51 | `VectorExample.java` | Legacy Vector, Enumeration |
| 52 | `StackExample.java` | Stack, Deque-as-Stack |
| 53 | `QueueExample.java` | Queue interface, LinkedList impl |
| 54 | `PriorityQueueExample.java` | PriorityQueue, custom comparator |
| 55 | `HashSetExample.java` | HashSet, hash/equals impact |
| 56 | `LinkedHashSetExample.java` | Insertion order preservation |
| 57 | `TreeSetExample.java` | Sorted & navigable set |
| 58 | `HashMapExample.java` | HashMap, computeIfAbsent, merge |
| 59 | `LinkedHashMapExample.java` | LRU cache, access order |
| 60 | `TreeMapExample.java` | Sorted map, subMap/headMap |
| 61 | `ConcurrentHashMapExample.java` | Thread-safe, compute, forEach |

### Phase 6 — Generics
| # | File | Topic |
|---|------|-------|
| 62 | `GenericClasses.java` | Generic type params |
| 63 | `GenericMethods.java` | Generic methods, type inference |
| 64 | `Wildcards.java` | `? extends`, `? super`, PECS |
| 65 | `BoundedTypes.java` | Multiple bounds |
| 66 | `TypeErasure.java` | Erasure, bridge methods, raw types |

### Phase 7 — Java 8+
| # | File | Topic |
|---|------|-------|
| 67 | `LambdaExpressions.java` | Lambda syntax, capture |
| 68 | `FunctionalInterfaces.java` | Predicate, Function, Consumer, Supplier |
| 69 | `MethodReferences.java` | All 4 method reference kinds |
| 70 | `StreamsAPI.java` | filter, map, flatMap, reduce, collect |
| 71 | `Optional.java` | of, orElse, map, flatMap, orElseThrow |
| 72 | `CompletableFuture.java` | supplyAsync, thenApply, thenCompose |
| 73 | `ParallelStreams.java` | parallelStream, forEachOrdered |

### Phase 8 — File Handling
| # | File | Topic |
|---|------|-------|
| 74 | `FileClass.java` | File, mkdir, list, delete |
| 75 | `BufferedReader.java` | readLine, lines(), mark/reset |
| 76 | `BufferedWriter.java` | write, newLine, append, flush |
| 77 | `NIOExample.java` | Path, Files, FileChannel, ByteBuffer |
| 78 | `Serialization.java` | ObjectOutputStream, transient |
| 79 | `Deserialization.java` | ObjectInputStream, versioning |
| 80 | `JSONParsing.java` | Self-contained JSON parser |

### Phase 9 — Multithreading
| # | File | Topic |
|---|------|-------|
| 81 | `81_ThreadClassExample.java` | Extend Thread, join, priority |
| 82 | `82_RunnableExample.java` | Runnable, lambda Runnable |
| 83 | `83_ExecutorServiceExample.java` | Fixed thread pool, submit |
| 84 | `84_FutureExample.java` | Future, get, cancel, timeout |
| 85 | `85_CallableExample.java` | Callable, invokeAll, invokeAny |
| 86 | `86_SynchronizationExample.java` | synchronized methods/blocks |
| 87 | `87_LocksExample.java` | ReentrantLock, Condition |
| 88 | `88_SemaphoresExample.java` | Semaphore, connection pool |
| 89 | `89_ProducerConsumerExample.java` | wait/notify, BlockingQueue |
| 90 | `90_ThreadPoolExample.java` | ThreadPoolExecutor, virtual threads |

### Phase 10 — Data Structures
| # | File | Topic |
|---|------|-------|
| 91 | `91_LinkedListDS.java` | Custom singly linked list |
| 92 | `92_StackDS.java` | Array-based stack |
| 93 | `93_QueueDS.java` | Circular queue |
| 94 | `94_DequeDS.java` | Doubly-linked deque |
| 95 | `95_HeapDS.java` | MinHeap & MaxHeap |
| 96 | `96_HashTableDS.java` | Separate chaining, rehashing |
| 97 | `97_BSTExample.java` | BST insert/search/delete |
| 98 | `98_AVLTreeExample.java` | AVL rotations, balance factor |
| 99 | `99_TrieExample.java` | Trie insert/search/startsWith |
| 100 | `100_GraphExample.java` | Adjacency list, BFS, DFS |

### Phase 11 — Algorithms
| # | File | Topic |
|---|------|-------|
| 101 | `101_BFS.java` | Breadth-first search |
| 102 | `102_DFS.java` | Depth-first search |
| 103 | `103_DijkstraAlgorithm.java` | Shortest path (non-negative) |
| 104 | `104_BellmanFordAlgorithm.java` | Shortest path (negative edges) |
| 105 | `105_FloydWarshallAlgorithm.java` | All-pairs shortest path |
| 106 | `106_KruskalAlgorithm.java` | MST with Union-Find |
| 107 | `107_PrimAlgorithm.java` | MST with PriorityQueue |
| 108 | `108_DynamicProgramming.java` | Knapsack, LCS, LIS |
| 109 | `109_Backtracking.java` | N-Queens, Sudoku, permutations |
| 110 | `110_GreedyAlgorithms.java` | Fractional knapsack, Huffman |

### Phase 12 — Databases
| # | File | Topic |
|---|------|-------|
| 111 | `111_JDBCExample.java` | JDBC connection, ResultSet |
| 112 | `112_ConnectionPooling.java` | Connection pool implementation |
| 113 | `113_TransactionsExample.java` | commit, rollback, savepoint |
| 114 | `114_PreparedStatementsExample.java` | Parameterized queries, batch |
| 115 | `115_ORMConcepts.java` | Custom ORM annotations |
| 116 | `116_HibernateBasics.java` | Hibernate SessionFactory, HQL |
| 117 | `117_JPAExample.java` | JPA EntityManager, JPQL |

### Phase 13 — Spring
| # | File | Topic |
|---|------|-------|
| 118 | `SpringCoreExample.java` | IoC, BeanFactory, @Component |
| 119 | `SpringBootExample.java` | @SpringBootApplication, REST |
| 120 | `DependencyInjection.java` | Constructor/Setter/Field injection |
| 121 | `RESTAPIs.java` | @RequestMapping, ResponseEntity |
| 122 | `ValidationExample.java` | @Valid, @NotNull, custom validator |
| 123 | `SecurityExample.java` | SecurityFilterChain, BCrypt |
| 124 | `JWTAuthentication.java` | JWT generation, validation |
| 125 | `JPAIntegration.java` | Spring Data JPA, @Query |
| 126 | `MicroservicesExample.java` | Patterns: gateway, discovery, CB |
| 127 | `APIGatewayExample.java` | Spring Cloud Gateway routes |
| 128 | `ServiceDiscoveryExample.java` | Eureka, @LoadBalanced |

### Phase 14 — Design Patterns
| # | File | Topic |
|---|------|-------|
| 129 | `SingletonPattern.java` | Eager, lazy, Bill Pugh, enum |
| 130 | `FactoryPattern.java` | Factory method pattern |
| 131 | `BuilderPattern.java` | Fluent builder, Director |
| 132 | `StrategyPattern.java` | Strategy with lambdas |
| 133 | `ObserverPattern.java` | Push/pull, PropertyChangeListener |
| 134 | `DecoratorPattern.java` | Stacked decorators |
| 135 | `AdapterPattern.java` | Class & object adapter |
| 136 | `CommandPattern.java` | Undo/redo, macros |
| 137 | `ChainOfResponsibility.java` | Middleware pipeline |

### Phase 15 — Systems
| # | File | Topic |
|---|------|-------|
| 138 | `138_CachingExample.java` | LRU cache, TTL, stats |
| 139 | `139_RedisIntegration.java` | SET/GET/EXPIRE, pub/sub |
| 140 | `140_KafkaProducerExample.java` | Partitioner, serializer, acks |
| 141 | `141_KafkaConsumerExample.java` | Consumer groups, offsets |
| 142 | `142_DistributedLocksExample.java` | SETNX, reentrant lock |
| 143 | `143_RateLimiterExample.java` | Token bucket, sliding window |
| 144 | `144_EventDrivenArchitecture.java` | Event bus, domain events |

### Phase 16 — Projects
| # | File | Lines | Description |
|---|------|-------|-------------|
| 145 | `145_LibraryManagement.java` | 350 | Books, members, borrow/return, fines |
| 146 | `146_BankingSystem.java` | 335 | Accounts, thread-safe transfers |
| 147 | `147_URLShortener.java` | 335 | Base62, collisions, analytics |
| 148 | `148_ChatApplication.java` | 340 | Rooms, DMs, virtual threads |
| 149 | `149_ECommerceBackend.java` | 445 | Cart, orders, payment state machine |
| 150 | `150_DistributedTaskScheduler.java` | 425 | Cron, retry, virtual thread workers |
| 151 | `151_MiniKafka.java` | 335 | Topics, partitions, consumer groups |
| 152 | `152_MiniSpringFramework.java` | 440 | DI container, @Component scanning |
| 153 | `153_SearchEngine.java` | 340 | Inverted index, TF-IDF, boolean queries |
| 154 | `154_TradingSystem.java` | 580 | Order book, price-time matching |
| 155 | `155_DistributedCache.java` | 375 | Consistent hashing, replication |
| 156 | `156_ProductionGradeMicroservicesPlatform.java` | 470 | Registry, load balancer, circuit breaker |

---

##  Quick Start

```bash
# Ensure you have JDK 21+
java --version

# Compile & run any program
cd Phase01_Basics
javac --release 21 01_HelloWorld.java
java 01_HelloWorld

# Or compile an entire phase
javac --release 21 -d /tmp/classes Phase02_OOP/*.java
java -cp /tmp/classes 16_ClassesAndObjects
```

> **Note:** Phase 12 (Databases) files compile with JDK-only APIs. To run against a real database, add a JDBC driver to the classpath. Phase 13 (Spring) files demonstrate concepts via self-contained code sketches (no Spring dependencies required at compile time).

---

##  Language Features Showcase

| Java Version | Features Used |
|-------------|---------------|
| **Java 21** | Virtual threads (`Thread.ofVirtual()`), record patterns, pattern matching for `switch`, sealed classes, sequenced collections |
| **Java 17+** | Sealed classes, pattern matching `instanceof`, records, text blocks |
| **Java 14+** | Switch expressions, helpful `NullPointerException` |
| **Java 11+** | `var` in lambdas, `Files.readString`/`writeString`, `isBlank`/`strip`/`repeat` |
| **Java 8+** | Lambdas, streams, `Optional`, `CompletableFuture`, default/static interface methods |

---

##  Project Structure

```
Java/
├── Phase01_Basics/             # 15 programs
├── Phase02_OOP/                # 15 programs
├── Phase03_Advanced_OOP/       # 10 programs
├── Phase04_Exception_Handling/ #  8 programs
├── Phase05_Collections/        # 13 programs
├── Phase06_Generics/           #  5 programs
├── Phase07_Java8Plus/          #  7 programs
├── Phase08_File_Handling/      #  7 programs
├── Phase09_Multithreading/     # 10 programs
├── Phase10_Data_Structures/    # 10 programs
├── Phase11_Algorithms/         # 10 programs
├── Phase12_Databases/          #  7 programs
├── Phase13_Spring/             # 11 programs
├── Phase14_Design_Patterns/    #  9 programs
├── Phase15_Systems/            #  7 programs
├── Phase16_Projects/           # 12 programs (300–580 lines each)
├── README.md
├── LICENSE
└── CODE_OF_CONDUCT.md
```

**156 programs — each is a standalone `.java` file with a `main()` method.**

---

##  Topics Covered

| Category | Topics |
|----------|--------|
| **Core Java** | Data types, operators, control flow, arrays, strings, methods, recursion |
| **OOP** | Classes, inheritance, polymorphism, abstraction, interfaces, composition, SOLID |
| **Advanced OOP** | Records, sealed classes, equals/hashCode, Comparable, Comparator, nested classes |
| **Exception Handling** | try-catch-finally, throw/throws, custom exceptions, try-with-resources |
| **Collections** | List, Set, Map, Queue, Deque, ConcurrentHashMap, stream operations |
| **Generics** | Generic classes/methods, wildcards, bounded types, type erasure |
| **Java 8+** | Lambdas, functional interfaces, streams, Optional, CompletableFuture |
| **I/O & NIO** | File, BufferedReader, BufferedWriter, NIO channels, serialization, JSON |
| **Concurrency** | Thread, Runnable, ExecutorService, Locks, Semaphores, virtual threads |
| **Data Structures** | Linked list, stack, queue, heap, hash table, BST, AVL, trie, graph |
| **Algorithms** | BFS, DFS, Dijkstra, Bellman-Ford, Floyd-Warshall, MST, DP, backtracking, greedy |
| **Databases** | JDBC, connection pooling, transactions, prepared statements, ORM, Hibernate, JPA |
| **Spring** | Core IoC, Boot, DI, REST APIs, validation, security, JWT, JPA, microservices |
| **Design Patterns** | Singleton, Factory, Builder, Strategy, Observer, Decorator, Adapter, Command, CoR |
| **Systems** | Caching, Redis, Kafka, distributed locks, rate limiting, event-driven architecture |
| **Projects** | Library, banking, URL shortener, chat, e-commerce, task scheduler, mini Kafka, search engine, trading system, distributed cache, microservices platform |

---

##  Contributing

Contributions are welcome! See the [contributing guidelines](CONTRIBUTING.md) for details.

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -am 'Add my feature'`
4. Push: `git push origin feature/my-feature`
5. Open a Pull Request

---

##  License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file.

---

<p align="center">
  Built with ❤️ for the Java community
</p>
