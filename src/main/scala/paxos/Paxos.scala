package paxos

import scala.concurrent.duration._

import cats.effect._
import cats.effect.kernel.Resource

import fs2.concurrent.SignallingRef
import middleware.{EventLog, Network}
import paxos.model.{Event, Message}
import paxos.model.Message.{ActivateAcceptor, ActivateProposer}
import paxos.process._

object Paxos extends IOApp.Simple {
  val ReadQuorum: Quorum = Quorum(2, "A1", "A2", "A3")
  val WriteQuorum: Quorum = Quorum(3, "A1", "A2", "A3")

  val run: IO[Unit] =
    for {
      signal <- SignallingRef[IO, Boolean](false)
      net <- Network[IO, Message]
      log <- EventLog[IO, Event]
      _ <- program(signal)(net, log)
    } yield ()

  def resources(
      signal: SignallingRef[IO, Boolean]
  )(implicit network: PaxosNetwork[IO], eventLog: PaxosEventLog[IO]): Resource[IO, Unit] =
    for {
      _ <- ProcessEngine.start[IO]("M1", MasterProcess)
      _ <- ProcessEngine.start[IO]("P1", ProposerProcess(ReadQuorum, WriteQuorum, crash = true))
      _ <- ProcessEngine.start[IO]("P2", ProposerProcess(ReadQuorum, WriteQuorum))
      _ <- ProcessEngine.start[IO]("P3", ProposerProcess(ReadQuorum, WriteQuorum))
      _ <- ProcessEngine.start[IO]("A1", AcceptorProcess)
      _ <- ProcessEngine.start[IO]("A2", AcceptorProcess)
      _ <- ProcessEngine.start[IO]("A3", AcceptorProcess)
      _ <- LearnerProcess.start[IO]("L1", 3, signal)
      _ <- ClientProcess.start[IO]("TEST")
    } yield ()

  def program(
      signal: SignallingRef[IO, Boolean]
  )(implicit network: PaxosNetwork[IO], eventLog: PaxosEventLog[IO]): IO[Unit] =
    resources(signal)
      .flatMap(_ => PaxosEventLog[IO].subscribe(100))
      .use { log =>
        for {
          fib <- log.interruptWhen(signal).evalMap(EventLogger.log[IO]).compile.drain.start
          _ <- PaxosNetwork[IO].broadcast(ActivateAcceptor("A1"))
          _ <- PaxosNetwork[IO].broadcast(ActivateAcceptor("A2"))
          _ <- PaxosNetwork[IO].broadcast(ActivateProposer("P1"))
          _ <- Temporal[IO].sleep(100.millis)
          _ <- PaxosNetwork[IO].broadcast(ActivateProposer("P2"))
          _ <- Temporal[IO].sleep(100.millis)
          _ <- PaxosNetwork[IO].broadcast(ActivateAcceptor("A3"))
          _ <- Temporal[IO].sleep(100.millis)
          _ <- PaxosNetwork[IO].broadcast(ActivateProposer("P3"))
          _ <- fib.join
        } yield ()
      }
}
