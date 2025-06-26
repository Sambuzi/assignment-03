file:///C:/assignment-03/ex2/src/main/scala/it/unibo/agar/protocol/FoodManagerProtocol.scala
### java.lang.AssertionError: assertion failed

occurred in the presentation compiler.

presentation compiler configuration:


action parameters:
uri: file:///C:/assignment-03/ex2/src/main/scala/it/unibo/agar/protocol/FoodManagerProtocol.scala
text:
```scala
package it.unibo.agar.protocol

import akka.actor.typed.ActorRef
import it.unibo.agar.model.Food

sealed trait FoodManagerMessage
case class GenerateFood(count: Int) extends FoodManagerMessage
case class RemoveFood(foodId: String) extends FoodManagerMessage
case class GetAllFood(replyTo: ActorRef[FoodList]) extends FoodManagerMessage
case class FoodList(foods: Seq[Food])

case class UpdateFood(foods: Seq[Food]) extends WorldManagerMessage
case object NoOp extends WorldManagerMessage
case object InitializeFoodManager extends WorldManagerMessage
case class PlayersUpdate(player: Set[ActorRef[PlayerActorMessage]]) extends WorldManagerMessage
case object ShuttingDown extends WorldManagerMessage
```



#### Error stacktrace:

```
scala.runtime.Scala3RunTime$.assertFailed(Scala3RunTime.scala:11)
	dotty.tools.dotc.core.Annotations$LazyAnnotation.tree(Annotations.scala:139)
	dotty.tools.dotc.core.Annotations$Annotation$Child$.unapply(Annotations.scala:245)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:476)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:482)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:482)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:482)
	dotty.tools.dotc.typer.Namer.addChild(Namer.scala:487)
	dotty.tools.dotc.typer.Namer$Completer.register$1(Namer.scala:931)
	dotty.tools.dotc.typer.Namer$Completer.registerIfChildInCreationContext$$anonfun$1(Namer.scala:940)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:334)
	dotty.tools.dotc.typer.Namer$Completer.registerIfChildInCreationContext(Namer.scala:940)
	dotty.tools.dotc.typer.Namer$Completer.complete(Namer.scala:829)
	dotty.tools.dotc.core.SymDenotations$SymDenotation.completeFrom(SymDenotations.scala:174)
	dotty.tools.dotc.core.Denotations$Denotation.completeInfo$1(Denotations.scala:188)
	dotty.tools.dotc.core.Denotations$Denotation.info(Denotations.scala:190)
	dotty.tools.dotc.core.Types$NamedType.info(Types.scala:2363)
	dotty.tools.dotc.core.Types$TermLambda.dotty$tools$dotc$core$Types$TermLambda$$_$compute$1(Types.scala:3896)
	dotty.tools.dotc.core.Types$TermLambda.foldArgs$2(Types.scala:3903)
	dotty.tools.dotc.core.Types$TermLambda.dotty$tools$dotc$core$Types$TermLambda$$_$compute$1(Types.scala:4524)
	dotty.tools.dotc.core.Types$TermLambda.dotty$tools$dotc$core$Types$TermLambda$$depStatus(Types.scala:3923)
	dotty.tools.dotc.core.Types$TermLambda.dependencyStatus(Types.scala:3937)
	dotty.tools.dotc.core.Types$TermLambda.isResultDependent(Types.scala:3959)
	dotty.tools.dotc.core.Types$TermLambda.isResultDependent$(Types.scala:3853)
	dotty.tools.dotc.core.Types$MethodType.isResultDependent(Types.scala:3998)
	dotty.tools.dotc.typer.TypeAssigner.assignType(TypeAssigner.scala:295)
	dotty.tools.dotc.typer.TypeAssigner.assignType$(TypeAssigner.scala:16)
	dotty.tools.dotc.typer.Typer.assignType(Typer.scala:119)
	dotty.tools.dotc.ast.tpd$.Apply(tpd.scala:48)
	dotty.tools.dotc.ast.tpd$TreeOps$.appliedToTermArgs$extension(tpd.scala:966)
	dotty.tools.dotc.ast.tpd$.New(tpd.scala:544)
	dotty.tools.dotc.ast.tpd$.New(tpd.scala:535)
	dotty.tools.dotc.core.Annotations$Annotation$Child$.makeChildLater$1(Annotations.scala:234)
	dotty.tools.dotc.core.Annotations$Annotation$Child$.later$$anonfun$1(Annotations.scala:237)
	dotty.tools.dotc.core.Annotations$LazyAnnotation.tree(Annotations.scala:143)
	dotty.tools.dotc.core.Annotations$Annotation$Child$.unapply(Annotations.scala:245)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:476)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:482)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:482)
	dotty.tools.dotc.typer.Namer.insertInto$1(Namer.scala:482)
	dotty.tools.dotc.typer.Namer.addChild(Namer.scala:487)
	dotty.tools.dotc.typer.Namer$Completer.register$1(Namer.scala:931)
	dotty.tools.dotc.typer.Namer$Completer.registerIfChildInCreationContext$$anonfun$1(Namer.scala:940)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:334)
	dotty.tools.dotc.typer.Namer$Completer.registerIfChildInCreationContext(Namer.scala:940)
	dotty.tools.dotc.typer.Namer$Completer.complete(Namer.scala:829)
	dotty.tools.dotc.core.SymDenotations$SymDenotation.completeFrom(SymDenotations.scala:174)
	dotty.tools.dotc.core.Denotations$Denotation.completeInfo$1(Denotations.scala:188)
	dotty.tools.dotc.core.Denotations$Denotation.info(Denotations.scala:190)
	dotty.tools.dotc.core.SymDenotations$SymDenotation.ensureCompleted(SymDenotations.scala:392)
	dotty.tools.dotc.typer.Typer.retrieveSym(Typer.scala:3092)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3117)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3231)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3302)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3306)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3328)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3374)
	dotty.tools.dotc.typer.Typer.typedPackageDef(Typer.scala:2914)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3184)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3232)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3302)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3306)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3417)
	dotty.tools.dotc.typer.TyperPhase.typeCheck$$anonfun$1(TyperPhase.scala:45)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	dotty.tools.dotc.core.Phases$Phase.monitor(Phases.scala:467)
	dotty.tools.dotc.typer.TyperPhase.typeCheck(TyperPhase.scala:51)
	dotty.tools.dotc.typer.TyperPhase.$anonfun$4(TyperPhase.scala:97)
	scala.collection.Iterator$$anon$6.hasNext(Iterator.scala:479)
	scala.collection.Iterator$$anon$9.hasNext(Iterator.scala:583)
	scala.collection.immutable.List.prependedAll(List.scala:152)
	scala.collection.immutable.List$.from(List.scala:685)
	scala.collection.immutable.List$.from(List.scala:682)
	scala.collection.IterableOps$WithFilter.map(Iterable.scala:900)
	dotty.tools.dotc.typer.TyperPhase.runOn(TyperPhase.scala:96)
	dotty.tools.dotc.Run.runPhases$1$$anonfun$1(Run.scala:315)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.ArrayOps$.foreach$extension(ArrayOps.scala:1324)
	dotty.tools.dotc.Run.runPhases$1(Run.scala:308)
	dotty.tools.dotc.Run.compileUnits$$anonfun$1(Run.scala:348)
	dotty.tools.dotc.Run.compileUnits$$anonfun$adapted$1(Run.scala:357)
	dotty.tools.dotc.util.Stats$.maybeMonitored(Stats.scala:69)
	dotty.tools.dotc.Run.compileUnits(Run.scala:357)
	dotty.tools.dotc.Run.compileSources(Run.scala:261)
	dotty.tools.dotc.interactive.InteractiveDriver.run(InteractiveDriver.scala:161)
	dotty.tools.pc.CachingDriver.run(CachingDriver.scala:45)
	dotty.tools.pc.WithCompilationUnit.<init>(WithCompilationUnit.scala:31)
	dotty.tools.pc.SimpleCollector.<init>(PcCollector.scala:351)
	dotty.tools.pc.PcSemanticTokensProvider$Collector$.<init>(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.Collector$lzyINIT1(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.Collector(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.provide(PcSemanticTokensProvider.scala:88)
	dotty.tools.pc.ScalaPresentationCompiler.semanticTokens$$anonfun$1(ScalaPresentationCompiler.scala:111)
```
#### Short summary: 

java.lang.AssertionError: assertion failed