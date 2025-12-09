---
trigger: always_on
---

# Design Principles and Programming Patterns

## Design Principles

### SOLID Principles
- **Single Responsibility Principle**: Each class should have only one reason to change
- **Open/Closed Principle**: Software entities should be open for extension but closed for modification
- **Liskov Substitution Principle**: Objects of a superclass should be replaceable with objects of its subclasses
- **Interface Segregation Principle**: Clients should not be forced to depend on interfaces they don't use
- **Dependency Inversion Principle**: Depend on abstractions, not on concretions

### Clean Architecture
- Separate business logic from framework concerns
- Dependency rule: source code dependencies point inward
- Independent of frameworks, UI, database, and external agencies

### KISS (Keep It Simple, Stupid)
- Simplicity is the ultimate sophistication
- Avoid unnecessary complexity
- Write code that is easy to understand and maintain

### DRY (Don't Repeat Yourself)
- Every piece of knowledge must have a single, unambiguous, authoritative representation
- Eliminate duplication through abstraction

### YAGNI (You Aren't Gonna Need It)
- Implement features only when they are actually needed
- Avoid premature optimization and over-engineering

## Programming Patterns

### Creational Patterns
- **Singleton**: Ensures a class has only one instance and provides a global point of access
- **Factory Method**: Defines an interface for creating an object, but lets subclasses alter the type of objects that will be created
- **Builder**: Separates the construction of a complex object from its representation

### Structural Patterns
- **Adapter**: Allows incompatible interfaces to work together
- **Decorator**: Adds behavior to objects dynamically
- **Facade**: Provides a simplified interface to a complex subsystem

### Behavioral Patterns
- **Observer**: Defines a one-to-many dependency between objects so that when one object changes state, all dependents are notified automatically
- **Strategy**: Defines a family of algorithms, encapsulates each one, and makes them interchangeable
- **Command**: Encapsulates a request as an object, thereby allowing for parameterization of clients with queues, requests, and operations

### Android-Specific Patterns
- **Model-View-ViewModel (MVVM)**: Separates development of graphical user interfaces from business logic
- **Repository Pattern**: Mediates data from multiple sources and provides a clean API to the rest of the application
- **Use Case (Interactor)**: Encapsulates business logic and provides a way to execute operations

## Kotlin/Android Specific Patterns
- **Extension Functions**: Add new functionality to existing classes without inheritance
- **Coroutines**: Handle asynchronous and non-blocking operations
- **Delegation**: Delegate property implementation to another object
- **Sealed Classes**: Represent restricted class hierarchies
- **Data Classes**: Automatically generate common methods like equals, hashCode, and toString

## Best Practices
- Use immutable data structures when possible
- Implement proper error handling and logging
- Follow naming conventions and code style guidelines
- Write unit tests for business logic
- Use dependency injection for better testability and maintainability
- Apply proper resource management and memory optimization
- Follow Android's lifecycle-aware components patterns