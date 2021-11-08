package paxos.process

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import paxos.model.{Event, Message}
import paxos.model.Event._
import paxos.model.Message._
import paxos.model.State.ProposerPhase._

class ProposerProcessSpec extends AnyWordSpec with Matchers {
  private val Proposer = new ProposerProcess(Quorum(2, "A1", "A2", "A3"), Quorum(3, "A1", "A2", "A3"))

  "ProposerProcess" when {
    "suspended" should {
      "activate proposer" in {
        val state = Proposer.init("P")
        val (nextState, messages, events) = Proposer.behavior((state, ActivateProposer("P")))
        nextState shouldBe state.copy(phase = Initializing)
        messages shouldBe Seq.empty[Message]
        events shouldBe Seq(NewProposerSelected("P"))
      }
    }
    "in initialization phase" should {
      "send prepare ballot (1a) message to acceptors" in {
        val state = Proposer.init("P").copy(phase = Initializing)
        val (nextState, messages, events) = Proposer.behavior((state, NewBallot(2, 1)))
        nextState shouldBe state.copy(phase = Preparing, ballot = 2, prevBallot = 1)
        messages shouldBe Seq(Prepare1a(2, 1))
        events shouldBe Seq(NewBallotCreated(state.id, 2, 1))
      }
    }
    "in preparing phase" should {
      "collect empty ballot promises (1b) from acceptors" in {
        val state = Proposer.init("P").copy(phase = Preparing, ballot = 2, prevBallot = 1)
        val (nextState, messages, events) = Proposer.behavior((state, Promise1b("A1", 2, 1, None)))
        nextState shouldBe state.copy(promises = Set("A1"))
        messages shouldBe Seq.empty[Message]
        events shouldBe Seq.empty[Event]
      }
      "complete ballot on a read quorum of empty ballot promises (1b) from acceptors" in {
        val state = Proposer.init("P").copy(phase = Preparing, ballot = 2, prevBallot = 1, promises = Set("A3"))
        val (nextState, messages, events) = Proposer.behavior((state, Promise1b("A1", 2, 1, None)))
        nextState shouldBe state.copy(phase = Activating, promises = Set("A1", "A3"))
        messages shouldBe Seq(Complete(2, 1))
        events shouldBe Seq.empty[Event]
      }
      "propose value for ballot on a non-empty ballot promise (1b) from acceptor" in {
        val value = "VALUE"
        val state = Proposer.init("P").copy(phase = Preparing, ballot = 2, prevBallot = 1, promises = Set("A3"))
        val (nextState, messages, events) = Proposer.behavior((state, Promise1b("A1", 2, 1, Some(value))))
        nextState shouldBe state.copy(phase = Proposing, safeVal = Some("VALUE"))
        messages shouldBe Seq(Propose2a(2, value))
        events shouldBe Seq(ValueProposed(state.id, 2, value))
      }
    }
    "in proposing phase" should {
      "collect votes on a ballot from acceptors" in {
        val state = Proposer.init("P").copy(phase = Proposing, ballot = 2, prevBallot = 1)
        val (nextState, messages, events) = Proposer.behavior((state, Accept2b("A1", 2)))
        nextState shouldBe state.copy(accepted = Set("A1"))
        messages shouldBe Seq.empty[Message]
        events shouldBe Seq.empty[Event]
      }
      "complete ballot on a write quorum of votes on a ballot from acceptors" in {
        val state = Proposer.init("P").copy(phase = Proposing, ballot = 2, prevBallot = 1, accepted = Set("A2", "A3"))
        val (nextState, messages, events) = Proposer.behavior((state, Accept2b("A1", 2)))
        nextState shouldBe state.copy(phase = Activating, accepted = Set("A1", "A2", "A3"))
        messages shouldBe Seq(Complete(2, 1))
        events shouldBe Seq(ReplicationCompleted(state.id, 2, 1))
      }
    }
    "in activating phase" should {
      "activate empty ballot and await a client request" in {
        val state = Proposer.init("P").copy(phase = Activating, ballot = 2, prevBallot = 1)
        val (nextState, messages, events) = Proposer.behavior((state, Activated(2)))
        nextState shouldBe state.copy(phase = AwaitingRequest)
        messages shouldBe Seq.empty[Message]
        events shouldBe Seq(BallotActivated(state.id, 2, voted = false))
      }
      "activate non-empty ballot and finish process" in {
        val value = "VALUE"
        val state = Proposer.init("P").copy(phase = Activating, ballot = 2, prevBallot = 1, safeVal = Some(value))
        val (nextState, messages, events) = Proposer.behavior((state, Activated(2)))
        nextState shouldBe state.copy(phase = Finished)
        messages shouldBe Seq.empty[Message]
        events shouldBe Seq(BallotActivated(state.id, 2, voted = true), ProposerFinished(state.id, 2))
      }
    }
    "in awaiting request phase" should {
      "propose requested value for ballot (2b) and finish" in {
        val value = "VALUE"
        val state = Proposer.init("P").copy(phase = AwaitingRequest, ballot = 2, prevBallot = 1)
        val (nextState, messages, events) = Proposer.behavior((state, ClientRequest(value)))
        nextState shouldBe state.copy(phase = Finished)
        messages shouldBe Seq(Propose2a(2, value))
        events shouldBe Seq(
          ClientRequestedValue(state.id, value),
          ValueProposed(state.id, 2, value),
          ProposerFinished(state.id, 2)
        )
      }
    }
  }
}
