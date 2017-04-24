<!-- 
Sílabas tônicas estão em negrito. Letras maiúsculas no meio de uma palavra indicam que a mesma é pronunciada
como no português. 

Várias palavras interligadas com hífen devem ser pronunciadas juntas (como uma terminando em consoante e outra começando em vogal).
-->

# Slide 1: Title
Good morning,

My name is Manoel Campos, I'm a PhD student at University of Beira Interior and a professor at a federal institute of education in Brazil.
I'm going to **prE**sent "CloudSim Plus: A cloud computing simulation **fra**mework pursuing software engineering principles for improved modularity, extensibility and correctness".

# Slide 2: Agenda
The following topics are going to be covered today:
- **An-introduction** to CloudSim Plus;
- It's architecture, modules and main packages;
- Main exclusive features;
- Conclusions and future work.

# Slide 3: Introduction
CloudSim Plus is **an-independent** CloudSim fork for cloud computing simulation which uses the most recent features from Java 8.
It's a highly extensible, completely redesigned and refactored **fra**mework, making it easier to create simulation scenarios.

It has more than 20 exclusive features, enabling implementation of complex and more realistic simulations.
It's heavily founded in Design Patterns, SOLID principles, Clean Code programming and other ones.

The **fra**mework sig**ni**ficantly reduces code duplication by 30%, removing redundancy to provide a simplified design. 
A side-by-side comparison between a simulation scenario in CloudSim and CloudSim Plus is available at this link. 
The link to the presentation is provided in the end.

Finally, it increases test coverage by 80%, while fixing **se**veral issues, providing more accuracy and safety to perform changes.

# Slide 4: Architecture
CloudSim Plus is a maven project available at maven central, enabling new tools to be built on **top-of-it** **in-an-easier** way.
It has a simplified module structure which is easier to understand and maintain. It also introduces some new modules.

It has a totally re-organized package structure for compliance with Separation of Concerns principle, 
placing only classes with the same goal into the same package. 

Finally, new interfaces are introduced to increase abstraction and define contracts for implementing classes. 

Researchers can rely on these public interfaces to create their simulations and build tools on **top-of** CloudSim Plus.

# Slide 5: Modules
CloudSim Plus is compounded of just 4 modules. The API module is the main, independent and single-required one for building simulations. 
All the other modules **depend-on-it**. 

The Examples module was updated for removal of code duplication and better organization, including some exclusive examples.
Testbeds and Benchmarks modules are new in CloudSim Plus. 

Testbeds module provides implementation of simulations which are executed multiple times, 
applying different seeds for pseudo random number generators 
and allowing collection and a**nA**lysis of scientifically valid results. 

The last module is used for performance assessment of cumbersome features such as heuristics. 

It enables a researcher to get metrics such as number of operations per second which may be used 
to guide the tunning of implemented algorithms and heuristics.

# Slide 6: Main Packages
The new packages structure makes it easier to find a given class. 
For instance, if you are looking for a Host implementation, you'll find it inside the hosts package.

The dark yellow packages includes exclusive CloudSim Plus features. 
Light yellow ones were introduced to better organize existing CloudSim classes.

Finally, white ones are existing CloudSim packages. 
However, new classes and interfaces were added and existing ones were updated to fix bugs, improve documentation, provide new features or improve the design.

# Slide 7: Main Exclusive Features
There are more than 20 exclusive features. 
Due to time limitation, only the most important ones are going to be presented.

The official website presents **an-extended** list.

# Slide 8: Vm Scaling
One of the most interesting new features is VM scaling. 
Vertical scaling enables specific resources of a VM, such as RAM or CPU, to be scaled up or down, 
according to current demand and a defined static or dynamic threshold. 

This way it allows fitting VM resources to current workload, aiming to reduce resource under and over provisioning, as well as SLA violations.

Horizontal scaling enables creating or destroying VM instances to balance the load, also according to defined thresholds. 

Since sometimes the Host where a VM is placed doesn't have enough resources to scale the VM up, 
or the vertical scaling is not enough to meet the demand, horizontal scale is one alternative for VM migration.

# Slide 9: Parallel Execution
Sometimes simulations may take long several minutes to run. 
Parallel execution enables multiple simulations to be run at the same time, 
in a multi-core CPU machine, which may speed up getting simulation results.

It relies on Java 8 Parallel Streams mechanism and using this feature 
may be as simple as calling a single line of code, like this one.

Here we have a list of simulation instances to be executed, and considering there is a method "run" that builds the simulation scenario and runs it,
such a line of code created the required threads to execute each simulation instance.

# Slide 10: Delay creation of submitted Cloudlets
