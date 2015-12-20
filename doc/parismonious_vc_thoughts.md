## Some thoughts on parsimonious vc generation

In our long employed VC generating scheme, the **While** condition and the loop
invariant false would be added to the hypothesis of all the VCs arising from
the loop body, and this would make them all valid (and marked as proven), as
Paul intended. However, Sami & Co. have been working on a strategy to produce
VCs which are more parsimonious in that it attempts to avoid the introduction of
irrelevant hypotheses during the VC generation process.

Recall that one faction of our group [the RSRG] has been viewing program
verification (and VC generation in particular) as a syntax directed reverse
proof generation process. Regarded in this way, there is one proof rule for
each statement type in the programming language, and when it is viewed in the
reverse direction, it tells how to eliminate statements of this type from the
penultimate position in a program. E.g., for the swap statement, the rule would
be:

```
C\ Confirm RD[x ~> y, y ~> x];
------------------------------
C\ x :=: y; Confirm RD;
```
where *code* is all prior statements in the program, and **RD** is the **R**esult
**D**escriptor