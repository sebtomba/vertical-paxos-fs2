package paxos.process

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import paxos.model.Event
import paxos.model.Message.{Activated, ActivateProposer, Complete, NewBallot}

class MasterProcessSpec extends AnyWordSpec with Matchers {
  "MasterProcess" when {
    "new proposer activated" should {
      "create new ballot" in {
        val state = MasterProcess.init("M")
        val (nextState, messages, events) = MasterProcess.behavior((state, ActivateProposer("P")))
        nextState shouldBe state.copy(nextBallot = state.nextBallot + 1)
        messages shouldBe Seq(NewBallot(state.nextBallot, state.ballot))
        events shouldBe Seq.empty[Event]
      }
    }
    "proposer active" should {
      "activate ballot when ballot is completed" in {
        val state = MasterProcess.init("M").copy(nextBallot = 2)
        val (nextState, messages, events) = MasterProcess.behavior((state, Complete(1, 0)))
        nextState shouldBe state.copy(ballot = 1)
        messages shouldBe Seq(Activated(1))
        events shouldBe Seq.empty[Event]
      }
    }
  }
}
