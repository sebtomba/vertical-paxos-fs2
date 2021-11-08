package paxos.process

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import paxos.model.Event._
import paxos.model.Message._
import paxos.model.Message

class AcceptorProcessSpec extends AnyWordSpec with Matchers {
  "AcceptorProcess" when {
    "not activated" should {
      "activate acceptor" in {
        val state = AcceptorProcess.init("A")
        val (nextState, messages, events) = AcceptorProcess.behavior((state, ActivateAcceptor("A")))
        nextState shouldBe state.copy(active = true)
        messages shouldBe Seq.empty[Message]
        events shouldBe Seq(AcceptorActivated("A"))
      }
    }
    "in preparation phase (1a)" should {
      "prepare ballot and return promise (1b) for previous vote ballot" in {
        val state = AcceptorProcess.init("A").copy(active = true, maxBallot = 1)
        val (nextState, messages, events) = AcceptorProcess.behavior((state, Prepare1a(2, 1)))
        nextState shouldBe state.copy(maxBallot = 2)
        messages shouldBe Seq(Promise1b(state.id, 2, 1, None))
        events shouldBe Seq(BallotPrepared(state.id, 2, 1, None))
      }
      "prepare ballot and return promise (1b) for previous vote ballot with value" in {
        val value = "B1"
        val state = AcceptorProcess.init("A").copy(active = true, votes = Map[Long, Any](1L -> value), maxBallot = 1)
        val (nextState, messages, events) = AcceptorProcess.behavior((state, Prepare1a(2, 1)))
        nextState shouldBe state.copy(maxBallot = 2)
        messages shouldBe Seq(Promise1b(state.id, 2, 1, Some(value)))
        events shouldBe Seq(BallotPrepared(state.id, 2, 1, Some(value)))
      }
      "ignore messages for old ballots" in {
        val state = AcceptorProcess.init("A").copy(active = true, maxBallot = 2)
        AcceptorProcess.behavior.isDefinedAt((state, Prepare1a(1, 0))) shouldBe false
      }
    }
    "in voting phase (2a)" should {
      "accept value" in {
        val value = "B2"
        val state = AcceptorProcess.init("A").copy(active = true, maxBallot = 2)
        val (nextState, messages, events) = AcceptorProcess.behavior((state, Propose2a(2, value)))
        nextState shouldBe state.copy(votes = state.votes + (2L -> value))
        messages shouldBe Seq(Accept2b(state.id, 2))
        events shouldBe Seq(ValueAccepted(state.id, 2, value))
      }
      "ignore messages for old ballots" in {
        val value = "B2"
        val state = AcceptorProcess.init("A").copy(active = true, maxBallot = 2)
        AcceptorProcess.behavior.isDefinedAt((state, Propose2a(1, value))) shouldBe false
      }
    }
  }
}
