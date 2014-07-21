akka-concurrency
================

 * For the book "Akka Concurrency"
 * A sample for the Typesafe Stack (http://typesafe.com/stack).
 * Akka 2.1.2 sample project using Scala and SBT.
 * To run and test it use SBT invoke: `sbt run` or `sbt test`


Code
====
 * Code is divided into two main dirs: `investigation` and `avionics`.
 * For `avionics`, `Avionics` has `main`.  It creates a `Plane` actor
   which in turn creates a hierarchy of other actors - pilot, flight attendants etc.



Run or Test
===========
Code after each relevant section is tagged.  The idea is to
git checkout the required tag and `sbt run` or `sbt test`.

More details are below.

### List all tags
```
$ git tag -l -n1
```


### Sec 5.5 Plane with Altimeter
```
$ git checkout sec5.5
$ sbt run
Enter number: 2       ## avionics.Avionics
```


### Sec 5.5 EventSource: Getting Updates from the Altimeter
```
$ git checkout sec5.5a
$ sbt run
Enter number: 2       ## avionics.Avionics
```


### Sec 6.3 Testing the EventSource
```
$ git checkout sec6.3
$ sbt test
```

### Sec 6.5 Running Tests in Parallel with Separate ActorSystems
```
$ git checkout sec6.5
$ sbt test

# Using TestProbe
$ git checkout sec6.5a
$ git diff sec6.5
$ sbt test
```


### Sec 7.2 Actor Paths
```
$ git checkout sec7.2
$ sbt run
Enter number: 4       ## investigation.SampleActorPath
```


### Sec 7.3 Section 7.3 Staffing the Plane: FlightAttendants
```
$ git checkout sec7.3
$ sbt run
Enter number: 4       ## avionics.FlightAttendantChecker
```


### Sec 7.4 Add Pilot and Copilot
```
$ git checkout sec7.4
$ sbt run
Enter number: 2       ## avionics.Avionics
```


### Sec 8.5 The Plane that Healed Itself - Supervisors
 * Modified hierarchy with StopSupervisor (Pilots) and ResumeSupervisor (Equipment).
   Overrides default actor life cycle.  The FlightAttendants, including the lead, use
   the default life cycle.  See Fig 8.7
 * Cake pattern for constructing the Plane.  Using *Provider traits and Plane self typing.
 * Dependency injection into Pilot using plane, autopilot, control and altimeter ActorRefs.
   Similarly for Copilot.

The old main programs and tests should still work.  There is nothing testing the added functionality yet.
```
$ git checkout sec8.5
$ sbt run
Enter number: 2       ## avionics.Avionics
```

#### Sec 8.5 Pilot Flies the Plane
```
$ git checkout sec8.5a
$ sbt
sbt> run pilot
Enter number: 2       ## avionics.Avionics 
```
You should see a message "Pilot will start flying the plane."


### Sec 8.6 Dead Pilots
 * Copilot watches Pilot.  Note that using `actorSelection` is a bit more complicated than
   using `actorFor`.
 * `PilotSpec` has the newly added test.
 ```
 $ git checkout sec8.6
 $ sbt
 sbt> testOnly PilotSpec
 ```



Actor Tradeoffs
===============
Using Actors comes with its benefits at a price.
 * `ActorRef`s are not typed.  Actor hierarchies can change over time.  Don't expect
   `context.parent` to be the same tomorrow as it is today.  Lack of type does not save
   you here.



Initial Setup
=============
```
$ g8 typesafehub/akka-scala-sbt -b akka-2.1-scala-2.10

Akka 2.1.2 Project Using Scala and sbt

name [Akka Project In Scala]: Akka Concurrency
organization [org.example]: com.jraman
version [0.1-SNAPSHOT]:
akka_version [2.1.2]:

Template applied in ./akka-concurrency
```

