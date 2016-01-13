## Notes on parsimonious vc generation
*The following is from an email written by Bill Ogden describing some thoughts, examples, and motivations for Clemson RSRG's new method of culling uneeded, extraneous verification conditions (VCs).*

### 11/14/2015

"In our long employed VC generating scheme, the **While** condition and the loop invariant false would be added to the hypothesis of all the VCs arising from the loop body, and this would make them all valid (and marked as proven), as Paul intended. However, Sami & Co. have been working on a strategy to produce VCs which are more parsimonious in that it attempts to avoid the introduction of irrelevant hypotheses during the VC generation process.

Recall that one faction of our group [the RSRG] has been viewing program verification (and VC generation in particular) as a syntax directed reverse proof generation process. Regarded in this way, there is one proof rule for each statement type in the programming language, and when it is viewed in the reverse direction, it tells how to eliminate statements of this type from the penultimate position in a program. E.g., for the swap statement, the rule would be:

```
C\ Confirm RD[x ~> y, y ~> x];
------------------------------
C\ x :=: y; Confirm RD;
```
where *code* is all prior statements in the program, and **RD** is the **R**esult **D**escriptor expression what needs to be true after any execution of the preceding program.

The rule for procedure calls has a little more complex effect on the result descriptor that's brought forward in that it typically involves both a pre and a post condition. A relational post condition adds auxiliary variables to stand for the values the updated parameters would have had after the eliminated procedure call and it adds as a hypothesis implying the target result descriptor. The pre condition, when instantiated with the actual parameters, becomes an additional conjunct to the result descriptor. The pre condition, when instantiated witht he actual parameters, becomes an additional conjunct to the result descriptor in the proof rule hypothesis. Given that, whenever things go badly, we need to trace back to the source of a proof obligation, that theorem proving is at best an exponentially complex process, and that result descriptors turn out to be far from arbitrary in logical structure. The mathematical proving process at the end of program verification typically involves breaking the final result descriptors produced by backing all the way up the various paths through the code into what is effectively a conjunction of Gentzen sequents `G_1, ... , G_n`, which in reality are just VCs produced by the reverse program proof process, but expressed in a proof friendly form. The sequents have the following form:

```
G_j = {A_(j, 1), ... , A_(j, m(j))} ==> {S_j}
```
where each `A_(j, k)` term is an antecedent, and `S_j` is the succedent in `G_j`. 

Semantically,

```
G_j ≈ (A_(j,1) and  A_(j, 2) and ... and A_(j, m(j))) ==> S_j
```

The problem with waiting to form the sequents is that a whole bunch of antecedents that are irrelevant to proving `S_j` tend to be accumulated. The strategy then involves keeping the result descriptors in the conjunction of Gentzen sequents formed throughout the reverse proof generation process. Now consider what happens to a particular sequent in RD when then penultimate statement in our program is a procedure invocation `P(B, C, D)`, where `B` and `C` are in `updated` parameter positions and `D` is in a `preserved` position. I.e.:

```
Code;
P(B, C, D);
Confirm G_1 and G_2 and ... and G_n;
```

Suppose it happens that `G_2` only makes reference to variables `D`, `E`, and `F`. Semantically then, it seems clear that the the truth value of `G_2` should depend solely on the values of `D`, `E`, and `F` produced by the *code* section of the program and that it is unaffected by the call to `P`. So a rule that merely required that `G_2` to be valid after code was executed should be sound. I.e., the reverse proof generation process could safely produce something that looked like:

```
Code;
Confirm G_1' and G_2' and ... and G_n' and G_n+1;
```

Where each `G_j` that involves variables `B` or `C` has been appropriately modified to `G_j'` and `G_n+1` accounts for `P`'s pre condition.

Now what would happen if we had the procedure equivalent of the "**maintaining** false" that evoked this discussion. Consider the implementationally challenging operation:

```
Operation Schizovalue(updates i : Int);
  ensures i = 6 and i = 9;   //a Jimi Hendrix reference
```

For a program:

```
Code;
    Schizovalue(B);
Confirm G_1 and G_2 and ... and G_n;
```
the new reverse proof generation process would again produce something that looked like:

```
Code;
Confirm G_1' and G_2' and ... and G_n' and G_n + 1;
```

and the antecedent portion of the VC `G_2` would contain no clue that it arose subsequent to the execution of an impossible operation, whereas the earlier reverse proof generation process would've added the two antecedents `B = 6` and `B = 9` to all of `G_1, ... , G_n`, thereby making them all trivially valid. Of course, the overriding verification problem for the program lies elsewhere in that there is no valid realization for Schizovalue. So if the VC ultimately generated from `G_2` by the new reverse proof generation process happens to be rejected by the mathematical theorem prover, then the programmar may learn about a problem wherever `G_2` arose beyond the `Schizovalue(B)` statement in his program.

The new generation process then is following a separation of concerns strategy with the goal of simplifying the mathematical proof process (and the debugging process, when that fails). The order in which bugs are identified may be different (and perhaps better :-) with this strategy, but as long as it works at least as well as the old strategy and never "proves" any invalid code, then it's OK.

So, what could go wrong? Well, in the old process, most of the antecedents in the result descriptors do arise from operation invocations, but a few arise from the control constructs of the language, and the simple rule of ignoring a potential antecedent `PA` if you're processing a sequent `GjGj` that has no variables in common with `PA`. Suppose for example, that you were verifying the code body: 

```
If y < 8 then x := x + 1;
If y <= y then x := x + 1;
```
which implements the operation:

```
Operation Weird_Increment(updates x : Int; preserves y : Int);
  requires x < Max_Int;
  ensures x = @x + 1;
```
The goal sequent at the end of *code* will be `{ } ==> {x = @x + 1}`, which involves `x`, but not `y`. The VC generation process will set up all four nominal paths through the code body. But if it doesn't add the conditions `y < 8` and `8 <= y` as antecedents to the appropriate sequents during the reverse proof process, then the sequents `{ } => {x + 2 = x + 1}` and `{ } => {x = @x + 1}` will go to the mathematical prover, and the new VC generation process will fail where the old one would have succeeded. So we must stipulate that the control predicates that appear in **If**, **While**, etc. statements always be included as antecedents. Loop invariants turn out to be like pre and post conditions, and can be treated accordingly."

## Hypothetical pseudocode


### Some test cases
