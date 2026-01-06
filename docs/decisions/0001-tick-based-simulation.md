# ADR-0001: Tick-Based Simulation

## Status
Accepted

## Context
We needed to choose a time model for the lift simulation. The main options were:
- **Real-time simulation**: Use actual wall-clock time (e.g., `Thread.sleep()`)
- **Event-driven simulation**: Advance time by jumping to the next scheduled event
- **Tick-based simulation**: Advance time in fixed discrete intervals (ticks)

Key requirements:
- Deterministic and reproducible behavior for testing
- Simple to understand and reason about
- Easy to step through and debug
- Support for unit testing without real-time delays

## Decision
We will use a **tick-based simulation** model where:
- Time advances in discrete ticks (each tick represents a fixed time unit)
- The `SimulationEngine` owns the current tick counter
- On each tick, the controller decides an action, which updates the lift state
- Tests can run thousands of ticks instantly without waiting on real time

## Consequences

### Positive
- **Deterministic**: Same inputs always produce same outputs, critical for testing
- **Fast testing**: Tests run at CPU speed, not wall-clock speed
- **Simple mental model**: Easy to understand "tick N produces state S"
- **Easy debugging**: Can step through tick-by-tick and inspect state changes
- **Reproducible**: Can record and replay exact tick sequences

### Negative
- **Artificial granularity**: Real time is continuous, ticks are discrete
- **Tick duration ambiguity**: What real-world time does one tick represent? (Not critical for Iteration 1)
- **Coordination challenge**: If adding multiple lifts later, need to ensure they advance in lockstep

### Neutral
- May need to map ticks to real time units (seconds/milliseconds) in future iterations
- Current implementation treats each tick as an abstract time unit
