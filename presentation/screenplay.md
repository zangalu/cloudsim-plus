# Slide 1: Title
Good morning,

My name is Manoel Campos, I'm a PhD student at Univeristy of Beira Interior and a professor at a federal institute of education in Brazil.
I'm going to present "CloudSim Plus: A Cloud Computing Simulation Framework Pursuing Software Engineering Principles for Improved Modularity, Extensibility and Correctness".

# Slide 2: Agenda
The agenda for today is as follows:
- An introduction to CloudSim Plus;
- It's architecture, modules and main packages;
- Main interfaces, some classes and exclusive features;
- Conclusions and future work.

# Slide 3: Introduction
CloudSim Plus is an independent CloudSim fork for cloud computing simulation which uses the most recent features from Java 8.
It's a highly extensible, completely redesigned and refactored framework that makes it easier to understand and create simulation scenarios.
It has more than 20 exclusive features to enable implementation of complex and more realistic simulations.
It's heavily founded in Design Patterns, SOLID principles, Clean Code programming and other ones.
The framework significantly reduces code duplication by 30%, removing redundancy to provide a simplified design. A side-by-side comparison between a simulation scenario in CloudSim and CloudSim Plus is available at this link. The link to the presentation will be provided at the end.
Furthermore, it increases test coverage by 83% while fixing several issues, providing more accuracy and safety to perform changes.

# Slide 4: Architecture

CloudSim Plus is a maven project available at maven central, enabling new tools to be built on top of it in a easier way.
It has a simplified module structure that is easier to understand and maintain. It also introduces some new modules.
It has a totally re-organized package structure for compliance with Separation of Concerns principle, placing only classes with the same goal at the same package. 
Furthermore, it introduces package documentation which are a excellent starting point to study the framework architecture.
Finally, new interfaces are introduced to increase abstraction and provide contracts for implementing classes. Researchers can rely on these public interfaces to create their simulations and build tools on top of CloudSim Plus.

# Slide 5: Modules

CloudSim Plus is compounded of just 4 modules. The API module is the main, independent and single-required module for building simulations. 
All the other modules depend on it. The examples module were updated for removal of code duplication and better organization, including some exclusive examples.
Testbeds and benchmarks modules are new in CloudSim Plus. The first one provides implementations of multiple-run simulations, enabling a simulation to be executed multiple times, applying different seeds for pseudo random number generators and allowing collection of scientifically valid data. The last module is used for performance assessment of cumbersome features such as heuristics. It enables a researcher to get measures such as number of operations per second which may be used to guide the tunning of implemented algorithms and heuristics.

# Slide 6: Main Packages
The new packages structure makes it easier to find a given class. For instance, if you are looking for a Host implementation, you'll intuitively find it inside the hosts package.
The dark yellow packages includes exclusive CloudSim Plus features. Light yellow ones were introduced to better organize existing CloudSim classes.
Finally, white ones are existing CloudSim packages, despite new classes and interfaces were added and existing ones were updated to fix bugs, improve documentation, provide new features or to improve the design.

# Slide 7: Main Interfaces and Classes
This diagram shows the main interfaces and some classes used to build simulation scenarios in CloudSim Plus. 
CloudSim Plus introduces actual relationships, there didn't exist in a object-oriented way, since they were just represented as integer IDs.

# Slide 8: Main Exclusive Features
There are more than 20 exclusive features. Due to time limitation, only the most important ones are going to be presented.
The official website presents an extended list.

# Slide 9: Vm Scaling
One of the most interesting new features is VM scaling. Vertical scaling enables specific resources of a VM, such as RAM or CPU, to be scaled up or down, according to current demand and a defined static or dynamic threshold. This way it allows fitting VM resources to current workload, aiming to reduce resource under and over provisioning, as well as SLA violations.

Horizontal scaling enables creating or destroying VM instances to balance the load, also according to defined thresholds. Since sometimes the Host where a VM is placed doesn't have enough resources to scale the VM up, or the vertical scaling is not enough to meet the demand, alternatively horizontal scale can be used.

# Slide 10: 

