// Doctor Agent
// 23rd November 2018 - Terry Payne
// This is the AgentSpeak code for the doctor agent, that is responsible
// for soliciting assistance from an agent to find and rescue victims, by
// exploiting the CONTRACT NET PROTOCOL.
//
// The use of the contract net protocol is based on the code that was 
// presented in the COMP329 Lectures: 24-25 Communication through AgentSpeak 

// ========================================================================
// Initial beliefs and rules
// ========================================================================
all_proposals_received(CNPId)
  :- .count(introduction(participant,_),NP) &
  				   // number of participants
     .count(propose(CNPId,_)[source(_)], NO) &
     			   // number of proposes received
     .count(refuse(CNPId)[source(_)], NR) &
     		       // number of refusals received
     NP = NO + NR.

// ========================================================================
// The following beliefs will be shared
location(hospital,0,0).
location(obstacle,1,1).
location(obstacle,1,2).
location(obstacle,4,1).
location(obstacle,3,3).
location(victim,2,5).
location(victim,1,3).
location(victim,0,5).
location(victim,5,5).
location(victim,4,0).

// ========================================================================
// The following beliefs MUST NOT be shared, but represent the location of
// actual victims and their critical status
critical(2,5).
~critical(1,3).
~critical(5,5).

// ========================================================================
// Initial goals
// ========================================================================
!startCNP(1,rescue).

// ========================================================================
// Plan Library
// ========================================================================
// Plans for handling the victim status.  Also checks the colour and victim status
+requestVictimStatus(X,Y,burgandy)[source(A)] : critical(X,Y)
	<-.send(A,tell,critical(X,Y)).
+requestVictimStatus(X,Y,cyan)[source(A)] : ~critical(X,Y)
    <- .send(A,tell,~critical(X,Y)).

// Plans for the CNP - taken from the contractor agent.  Note that the code
// has been modified sligntly to make it more relevant to Assignment 2.  In
// particular - it passes the numbers of critical and non critical victims
// in the task request.

// Start the CNP
+!startCNP(Id,Task)
   <- .print("Awaiting introductions before requesting the task: ",Task,"...");
      .wait(2000);  				// wait participants introduction
      +cnp_state(Id,propose);		// remember the state of the CNP
      .findall(Name,introduction(participant,Name),LP);
      .count(critical(_,_),C);		// Determine the number of critical victims
      .count(~critical(_,_),NC);	// Determine the number of non-critical victims
      .send(LP,tell,cfp(Id,Task,C,NC));
      .wait(all_proposals_received(CNPId), 4000, _); // wait 4s or for a response
      !contract(Id).

// this plan needs to be atomic so as not to accept
// proposals or refusals while contracting
@lc1[atomic] +!contract(CNPId)
   :  cnp_state(CNPId,propose)
   <- -+cnp_state(CNPId,contract);	// Track cnp state
      .findall(offer(O,A),propose(CNPId,O)[source(A)],L);
      L \== [];						// Ensure we have at least one offer
      .min(L,offer(WOf,WAg));		// and deal with the smallest
      !announce_result(CNPId,L,WAg);
      -+cnp_state(CNPId,finished).	// Track cnp state

// nothing todo, the current phase is not 'propose'
@lc2 +!contract(_).

-!contract(CNPId)
   <- .print("CNP ",CNPId," has failed!").

// Terminate the recursion when we have no more
// agents participating in the CFP
+!announce_result(_,[],_).

// announce to the winner
+!announce_result(CNPId,[offer(_,WAg)|T],WAg)
   <- .send(WAg,tell,accept_proposal(CNPId));
      !announce_result(CNPId,T,WAg).
      
// announce to others
+!announce_result(CNPId,[offer(_,LAg)|T],WAg)
   <- .send(LAg,tell,reject_proposal(CNPId));
      !announce_result(CNPId,T,WAg).
      