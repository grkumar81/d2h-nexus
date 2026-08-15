# SOLID & OOP Design Rules

## Purpose

All application code must follow sound Object-Oriented Programming (OOP) principles and SOLID design principles wherever they are applicable.

These rules are intended to keep the code maintainable, extensible, testable, readable, and loosely coupled. Do not introduce abstractions or design patterns only to satisfy a rule; apply them when they provide a clear design benefit.

---

## 1. OOP Principles

### 1.1 Encapsulation

- Keep an object's state private and expose behavior through well-defined methods.
- Do not expose mutable internal collections or implementation details unnecessarily.
- Prefer meaningful domain methods over direct manipulation of object state.
- Validate object state at appropriate boundaries.
- Avoid public setters when an object's state should only change through controlled operations.

**Prefer:**

```java
order.addItem(item);
order.cancel();
```

**Avoid:**

```java
order.setItems(items);
order.setStatus("CANCELLED");
```

---

### 1.2 Abstraction

- Expose only the behavior required by the caller.
- Hide implementation details behind interfaces or appropriate abstractions when multiple implementations or independent variation are expected.
- Do not create interfaces for every class without a real abstraction requirement.
- Keep abstractions focused on business or technical responsibilities.

---

### 1.3 Inheritance

- Use inheritance only when there is a genuine **IS-A** relationship.
- A subclass must be safely substitutable for its parent.
- Prefer composition over inheritance when behavior needs to be assembled or changed independently.
- Avoid deep inheritance hierarchies.
- Do not use inheritance merely to reuse a few methods.

**Prefer:**

```java
class OrderService {
    private final PaymentProcessor paymentProcessor;
}
```

over creating inheritance solely for code reuse.

---

### 1.4 Polymorphism

- Prefer polymorphism when behavior varies based on type, strategy, provider, or business rule.
- Avoid large conditional blocks that repeatedly determine behavior based on type.

**Avoid:**

```java
if (type.equals("CARD")) {
    // ...
} else if (type.equals("PAYPAL")) {
    // ...
} else if (type.equals("BANK")) {
    // ...
}
```

When the behavior is expected to grow or vary independently, consider an appropriate abstraction such as Strategy or Factory.

---

## 2. SOLID Principles

### 2.1 Single Responsibility Principle (SRP)

A class, method, or module should have one clear responsibility and one primary reason to change.

Rules:

- Do not create large "god classes".
- Separate business logic, persistence, validation, mapping, communication, and formatting responsibilities when they have independent reasons to change.
- Keep controllers focused on request/response orchestration.
- Keep repositories focused on data access.
- Keep services focused on business/application logic.
- Keep DTOs focused on data transfer.
- Extract responsibilities when a class becomes difficult to understand, test, or change.

**Warning signs:**

- A class has many unrelated dependencies.
- A class contains database access, business rules, HTTP calls, and formatting.
- A method performs validation, persistence, transformation, and external communication.
- Changes in unrelated requirements repeatedly require modification of the same class.

---

### 2.2 Open/Closed Principle (OCP)

Software components should be open for extension but closed for unnecessary modification.

Rules:

- When new behavior is expected to be added frequently, design an extension point instead of repeatedly modifying a large conditional block.
- Use Strategy, Factory, Template Method, Chain of Responsibility, or another suitable pattern when it naturally fits the variation.
- Do not over-engineer stable code merely because future changes are theoretically possible.
- Prefer adding a new implementation over modifying existing stable implementations when the business variation is clear.

---

### 2.3 Liskov Substitution Principle (LSP)

A subtype must be usable wherever its parent type is expected without breaking correctness.

Rules:

- Do not override methods in a way that violates the parent's contract.
- Do not throw unexpected unsupported-operation exceptions from methods that the parent contract promises to support.
- Preserve expected input/output behavior and invariants.
- Avoid inheritance when subclasses cannot honor the complete contract of the parent.
- Prefer composition when a subtype would need to weaken or redefine the parent's behavior.

---

### 2.4 Interface Segregation Principle (ISP)

Clients should not be forced to depend on methods they do not use.

Rules:

- Keep interfaces small and cohesive.
- Do not create large interfaces containing unrelated operations.
- Split interfaces when different clients need different subsets of behavior.
- Avoid implementations containing many meaningless or unsupported methods.

**Prefer:**

```java
interface OrderReader {
    Order getOrder(Long id);
}

interface OrderWriter {
    void save(Order order);
}
```

over forcing every consumer to depend on a large interface containing unrelated operations.

---

### 2.5 Dependency Inversion Principle (DIP)

High-level business logic should not depend directly on low-level implementation details.

Rules:

- Depend on abstractions where the dependency represents a meaningful variation or boundary.
- Inject dependencies rather than creating infrastructure dependencies directly inside business classes.
- Keep business logic independent from concrete implementations where practical.
- Use dependency injection consistently in Spring applications.
- Avoid `new` for infrastructure dependencies such as repositories, HTTP clients, message producers, or external service clients inside business logic.

**Prefer:**

```java
class OrderService {
    private final PaymentProcessor paymentProcessor;

    OrderService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }
}
```

over directly constructing a concrete infrastructure dependency inside `OrderService`.

---

## 3. Composition Over Inheritance

Use composition when:

- Behavior needs to change independently.
- Multiple behaviors need to be combined.
- Runtime substitution is required.
- There is no strong IS-A relationship.
- Inheritance would create a deep or fragile hierarchy.

Inheritance is acceptable when the domain relationship and substitutability are clear.

---

## 4. Prefer Behavior Over Data Manipulation

Domain and business objects should expose meaningful operations rather than requiring callers to manipulate their internal state.

**Prefer:**

```java
account.withdraw(amount);
```

**Avoid:**

```java
account.setBalance(account.getBalance().subtract(amount));
```

when the balance change represents a business operation.

---

## 5. Avoid Primitive Obsession Where Appropriate

Do not unnecessarily represent important domain concepts using unrelated primitive values.

For meaningful domain concepts such as:

- Money
- EmailAddress
- ProductCode
- CustomerId
- OrderStatus
- DateRange

consider dedicated value objects or enums when they provide validation, type safety, or domain behavior.

Do not create value objects for trivial fields without a clear benefit.

---

## 6. Avoid Boolean and Flag-Driven Design

Avoid methods with many boolean parameters that make behavior unclear.

**Avoid:**

```java
processOrder(order, true, false, true);
```

Prefer meaningful options, objects, enums, or separate operations when the behavior represents distinct business concepts.

---

## 7. Keep Methods Focused

Methods should perform one coherent operation.

Rules:

- Avoid very long methods.
- Avoid deeply nested conditionals.
- Extract meaningful operations when they improve readability and testability.
- Do not extract every few lines into a method without improving cohesion or readability.
- Method names should describe behavior, not implementation mechanics.

---

## 8. Minimize Coupling

- Classes should depend only on what they actually need.
- Avoid unnecessary knowledge of internal implementation details of other classes.
- Prefer dependency injection.
- Avoid static global state for application behavior.
- Avoid circular dependencies.
- Keep module boundaries clear.

---

## 9. Use Design Patterns Where Applicable

Use established design patterns when they solve an identifiable design problem.

Potential patterns include:

- **Strategy** — interchangeable business algorithms or rules.
- **Factory / Factory Method** — controlled creation of different implementations.
- **Abstract Factory** — creation of related object families.
- **Builder** — construction of complex objects.
- **Adapter** — integrating incompatible interfaces.
- **Facade** — simplifying access to a complex subsystem.
- **Decorator** — adding behavior without modifying the original implementation.
- **Chain of Responsibility** — sequential processing or validation.
- **Template Method** — common algorithm structure with controlled variations.
- **Observer / Event-driven patterns** — reacting to state changes or domain events.
- **Command** — encapsulating operations as objects.
- **State** — behavior that changes according to object state.

### Pattern Selection Rule

Do not use a design pattern simply because it exists.

Before introducing a pattern, identify:

1. What design problem exists?
2. What variation needs to be isolated?
3. Why is the pattern better than a simpler solution?
4. Does the pattern reduce coupling or improve extensibility/testability?
5. Does the additional abstraction justify its complexity?

If the answer is no, prefer the simpler design.

---

## 10. Avoid Over-Engineering

SOLID does not mean creating an interface, factory, strategy, and abstract class for every class.

Rules:

- Start with the simplest design that satisfies current requirements.
- Introduce abstractions when there is a real boundary or variation.
- Refactor toward a pattern when repeated change demonstrates the need.
- Avoid speculative abstractions.
- Prefer clear code over unnecessary architectural complexity.

---

## 11. Conditional Logic Rule

When conditional logic represents stable, small business logic, a simple `if`/`switch` is acceptable.

Consider Strategy, Factory, State, or another pattern when:

- The number of variants is growing.
- Each variant contains substantial independent behavior.
- Variants change independently.
- The same conditional logic is duplicated across multiple classes.
- Adding a new variant repeatedly requires modifying many existing classes.

---

## 12. Dependency Injection Rule

For Spring applications:

- Prefer constructor injection.
- Avoid field injection.
- Inject abstractions when abstraction provides meaningful decoupling.
- Do not hide dependencies through static accessors or service locators.
- Keep dependency graphs understandable.
- Avoid injecting a large number of unrelated dependencies into one class; this is often a sign of SRP violations.

---

## 13. Domain Logic Placement

Place behavior close to the data and responsibility it belongs to.

- Domain rules should not unnecessarily live in controllers.
- Controllers should not contain complex business logic.
- Repositories should not contain business decisions.
- Utility classes should not become a dumping ground for unrelated business logic.
- Services should orchestrate business operations and collaborate with appropriate domain components.

---

## 14. Code Review Checklist

Before considering implementation complete, verify:

- [ ] Does each class have a clear responsibility?
- [ ] Does each method have a clear purpose?
- [ ] Are responsibilities separated appropriately?
- [ ] Is inheritance genuinely required?
- [ ] Would composition be simpler?
- [ ] Are interfaces cohesive?
- [ ] Are high-level components unnecessarily coupled to concrete implementations?
- [ ] Are dependencies injected appropriately?
- [ ] Is there repeated conditional logic that should become polymorphism?
- [ ] Is a design pattern actually solving a real problem?
- [ ] Is the abstraction justified by current or clearly expected variation?
- [ ] Is the code over-engineered?
- [ ] Can the code be unit tested without excessive mocking or infrastructure setup?
- [ ] Are business rules located in the appropriate layer/component?
- [ ] Does the design remain understandable to another developer?

---

## 15. General Rule

**Apply OOP, SOLID principles, and design patterns where they provide a meaningful improvement in maintainability, extensibility, testability, readability, or separation of concerns.**

Do not blindly apply principles or patterns. The goal is **good design**, not maximum abstraction.

When reviewing or implementing code, explicitly identify SOLID/OOP violations when they materially affect the design and refactor them where appropriate.
