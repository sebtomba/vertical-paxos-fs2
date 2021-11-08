package paxos.process

import paxos.model.{Event, Message}
import paxos.model.Event._
import paxos.model.Message._
import paxos.model.State.{Proposer, ProposerPhase}
import paxos.model.State.ProposerPhase._

class ProposerProcess(readQuorum: Quorum, writeQuorum: Quorum, crash: Boolean = false) extends Process {
  override type State = Proposer

  def behavior: Behavior[Proposer] = {
    val start = suspendedPhase orElse initializingPhase
    if (crash) start
    else start orElse preparingPhase orElse proposingPhase orElse activatingPhase orElse awaitingRequestPhase
  }

  private def check(state: Proposer, phase: ProposerPhase, ballot: Long): Boolean =
    state.phase == phase && state.ballot == ballot

  private val suspendedPhase: Behavior[Proposer] = {
    case (state, ActivateProposer(proposer)) if state.phase == Suspended && state.id == proposer =>
      val nextState = state.copy(phase = Initializing)
      val events = Seq(NewProposerSelected(state.id))
      (nextState, Seq.empty, events)
  }

  private val initializingPhase: Behavior[Proposer] = {
    case (state, NewBallot(ballot, prevBallot)) if state.phase == Initializing =>
      val nextState = state.copy(phase = Preparing, ballot = ballot, prevBallot = prevBallot)
      val messages = Seq(Prepare1a(ballot, prevBallot))
      val events = Seq(NewBallotCreated(state.id, ballot, prevBallot))
      (nextState, messages, events)
  }

  private val preparingPhase: Behavior[Proposer] = {
    case (state, Promise1b(acceptor, ballot, voteBallot, promised))
        if check(state, Preparing, ballot) && state.prevBallot == voteBallot =>
      promised match {
        case Some(value) =>
          val nextState = state.copy(phase = Proposing, safeVal = Some(value))
          val messages = Seq(Propose2a(ballot, value))
          val events = Seq(ValueProposed(state.id, ballot, value))
          (nextState, messages, events)

        case _ =>
          val promises = state.promises + acceptor
          if (readQuorum(promises)) {
            val nextState = state.copy(phase = Activating, promises = promises)
            val messages: Seq[Message] = Seq(Complete(ballot, state.prevBallot))
            (nextState, messages, Seq.empty[Event])
          } else {
            val nextState = state.copy(promises = promises)
            (nextState, Seq.empty, Seq.empty)
          }
      }
  }

  private val proposingPhase: Behavior[Proposer] = {
    case (state, Accept2b(acceptor, ballot)) if check(state, Proposing, ballot) =>
      val accepted = state.accepted + acceptor
      if (writeQuorum(accepted)) {
        val nextState = state.copy(phase = Activating, accepted = accepted)
        val messages: Seq[Message] = Seq(Complete(ballot, state.prevBallot))
        val events = Seq(ReplicationCompleted(state.id, ballot, state.prevBallot))
        (nextState, messages, events)
      } else {
        val nextState = state.copy(accepted = accepted)
        (nextState, Seq.empty, Seq.empty)
      }
  }

  private val activatingPhase: Behavior[Proposer] = {
    case (state, Activated(ballot)) if check(state, Activating, ballot) =>
      if (state.safeVal.isEmpty) {
        val nextState = state.copy(phase = AwaitingRequest)
        val events = Seq(BallotActivated(state.id, ballot, voted = false))
        (nextState, Seq.empty, events)
      } else {
        val nextState = state.copy(phase = Finished)
        val events = Seq(BallotActivated(state.id, ballot, voted = true), ProposerFinished(state.id, state.ballot))
        (nextState, Seq.empty, events)
      }
  }

  private val awaitingRequestPhase: Behavior[Proposer] = {
    case (state, ClientRequest(value)) if check(state, AwaitingRequest, state.ballot) =>
      val nextState = state.copy(phase = Finished)
      val messages = Seq(Propose2a(state.ballot, value))
      val events = Seq(
        ClientRequestedValue(state.id, value),
        ValueProposed(state.id, state.ballot, value),
        ProposerFinished(state.id, state.ballot)
      )
      (nextState, messages, events)
  }

  def init(id: String): Proposer = Proposer(id, Suspended, 0, 0, None, Set.empty, Set.empty)
}

object ProposerProcess {
  def apply(readQuorum: Quorum, writeQuorum: Quorum, crash: Boolean = false): ProposerProcess =
    new ProposerProcess(readQuorum, writeQuorum, crash)
}
