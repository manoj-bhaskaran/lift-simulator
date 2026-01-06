# Lift Simulator

A Java-based simulation of lift (elevator) controllers with a focus on correctness and design clarity.

## Version

Current version: **0.1.3**

This project follows [Semantic Versioning](https://semver.org/). See [CHANGELOG.md](CHANGELOG.md) for version history.

## What is this?

The Lift Simulator is an iterative project to model and test lift controller algorithms. It simulates:
- Lift state and movement (floor position, direction, door state)
- Controller decision logic (choosing which floor to service next)
- Request handling (hall calls and car calls)
- Time-based simulation using discrete ticks

The simulation is text-based and designed for clarity over visual appeal.

## Iteration 1 Scope

The current iteration (v0.1.3) implements:
- **Single lift simulation** operating between configurable floor ranges
- **Tick-based simulation engine** that advances time in discrete steps
- **NaiveLiftController** - A simple controller that services the nearest pending request
- **Console output** displaying tick-by-tick lift state (floor, direction, door state)
- **Basic request types**: Car calls (from inside the lift) and hall calls (from a floor)

Future iterations will add multi-lift systems, smarter algorithms, and more realistic constraints.

## Prerequisites

- Java 17 or later
- Maven 3.6 or later

## Development Setup

This project includes an `.editorconfig` file to maintain consistent code formatting across different editors and IDEs. Most modern editors support EditorConfig either natively or through plugins:

- **IntelliJ IDEA**: Built-in support (no plugin needed)
- **VS Code**: Install the "EditorConfig for VS Code" extension
- **Eclipse**: Install the EditorConfig Eclipse plugin
- **Vim/Neovim**: Install the editorconfig-vim plugin

The configuration enforces:
- UTF-8 encoding
- LF line endings (ensures compatibility across Windows, Linux, and macOS)
- 4-space indentation for Java and XML files
- Trailing whitespace removal
- Final newline insertion

## Building the Project

Compile the project using Maven:

```bash
mvn clean compile
```

To build a JAR package:

```bash
mvn clean package
```

The packaged JAR will be in `target/lift-simulator-0.1.3.jar`.

## Running the Simulation

Run the demo simulation:

```bash
mvn exec:java -Dexec.mainClass="com.liftsimulator.Main"
```

Or run directly after building:

```bash
java -cp target/lift-simulator-0.1.3.jar com.liftsimulator.Main
```

The demo runs a pre-configured scenario with several lift requests and displays the simulation state at each tick.

## Running Tests

Execute all tests:

```bash
mvn test
```

Run tests with coverage:

```bash
mvn test jacoco:report
```

## Project Structure

```
src/
├── main/java/com/liftsimulator/
│   ├── Main.java                    # Entry point and demo
│   ├── domain/                      # Core domain models
│   │   ├── Action.java              # Actions the lift can take
│   │   ├── CarCall.java             # Request from inside lift
│   │   ├── Direction.java           # UP, DOWN, IDLE
│   │   ├── DoorState.java           # OPEN, CLOSED
│   │   ├── HallCall.java            # Request from a floor
│   │   └── LiftState.java           # Immutable lift state
│   └── engine/                      # Simulation engine and controllers
│       ├── LiftController.java      # Controller interface
│       ├── NaiveLiftController.java # Simple nearest-floor controller
│       ├── SimpleLiftController.java# Alternative basic controller
│       └── SimulationEngine.java    # Tick-based simulation engine
└── test/java/com/liftsimulator/
    └── ...                          # Unit tests
```

## Architecture Decisions

See [docs/decisions](docs/decisions) for Architecture Decision Records (ADRs):
- [ADR-0001: Tick-Based Simulation](docs/decisions/0001-tick-based-simulation.md)

## License

MIT License - see [LICENSE](LICENSE) file for details.
