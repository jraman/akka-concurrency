
## Sec 8.5

### Runtime Exception During Creation
Got the following *runtime* error:
```
com.jraman.avionics.Plane cannot be cast to com.jraman.avionics.LeadFlightAttendantProvider
akka.actor.ActorInitializationException: exception during creation
	at akka.actor.ActorInitializationException$.apply(Actor.scala:164)
	...
Caused by: java.lang.ClassCastException: com.jraman.avionics.Plane cannot be cast to com.jraman.avionics.LeadFlightAttendantProvider
	at com.jraman.avionics.Plane$$anonfun$startPeople$1.apply(Plane.scala:107)
    ...
```

In `Avionics.scala`, it turns out that the plane was constructed as:
```
system.actorOf(Props[Plane], ...)
```

But, with the changes for this section, it needs to be:
```
system.actorOf(Props(new Plane() with AltimeterProvider with PilotProvider with LeadFlightAttendantProvider), ...)
```

This error is not caught at compilation time.  Only at runtime.

This must be a consequence of the cake pattern?

Moral of the story: type checking only goes so far.


### Altimeter Updates Not Working
On another attempt, the program compiled and ran to completion.  But, the Altitude updates were all 0.0.

The error was in the name used in `actorFor`.  Here is the log line with the key piece of information:
```
[DEBUG] [07/17/2014 11:16:03.692] [PlaneSimulation-akka.actor.default-dispatcher-2] [LocalActorRefProvider(akka://PlaneSimulation)] look-up of path sequence [/ControlSurfaces] failed
```

Strangely, this is at the `DEBUG` level.  Would have expected at least a `WARNING` level.

The code change required is below:
```
-      val controls = context.actorFor("ControlSurfaces")
+      val controls = context.actorFor("Equipment/ControlSurfaces")
```

By the way, `actorFor` is deprecated in favor of `actorSelection`.  Will need to look into the behavior when
using `actorSelection`.

Moral of the story: Watch out for `actorFor`.



### Unhandled Messages Could Get Lost
If you send a message to an actor and the actor does not handle that case (i.e. the receive method partial function is undefined for that message),
then that message is as good as lost in the ether.  It does not go to dead letters.

How found: I misspelled the path of an actor and that actor was not getting the message that I sent.  And for the life of me,
could not figure out why the message was not being received.  The misspelled actor was receiving the message and silently
ignoring it.  I tried `actorFor`, `actorSelection`, `tell`, and `ask` (which timed out).

Moral of the story: Always have a `case _ =>` in the `receive` method of the actor that at least logs an error.

