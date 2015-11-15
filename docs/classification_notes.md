# A Math Type System for RESOLVE

This is a collection of notes, compiled email chains, talks, and snippets of Google+ chats with Bill Ogden, Joan Krone, and other RSRG members regarding the RESOLVE math classification checker.

## General Points

- Locate, and point out, gross errors quickly.

- Simplify semantics by eliminating the need to interpret gibberish.

- Makes theorem provers simpler.

- Needs to be more permissive than programming language type checking (e.g. `5 : N`, `5 : Z`, `5 : C`).

- Needs to be a natural formalization of traditional informal conventions of mathematical presentation. Must be mathematician (and cs grad) friendly.

- Should prevent mathematically bizarre "accidental" results from traditional set theoretic developments of math foundations (such as `3 : 7` or `{x} : <x, y>`) from being expressible.

- It seems to be tricky to cap off a type system cleanly, since for any type universe `U` that seems to be adequate, `U : U` isn't a logically sound possibility, and there inevitably seems to be a need for a larger, `BigU`, such that `U : BigU`

## The Mystery Role of the ':'

- What is the difference between `x : R` and `x in R`?

- Is `:` just a funny syntactic marker? Traditional mathematical discourse only uses it for function typing, as in `f : S -> T`. Does it carry some distinctive semantics?

- If we had a mathematical type system, would we know it was "right"?

- Is there an abstract metamathematical notion of precisely what sorts of calamitous outcomes a type system is intended to preclude from arising in a mathematical language?

- Is there a relatively simple syntactic specification of what constitutes a type-conformal mathematical expression?

- If so, can it be used to specify and verify an implementation of a classification checker?

- And also, can it be proven that type-conformal expressions do indeed preclude the calamitous outcomes?

## Hypothetical RESOLVE Math Universe Diagrams

Coming soon*

## Hypothetical Deduction Calculus for Classification

Instance 1:

    C \ f : C * D -> R
    C \ E1 : C
    C \ E2 : D
    --------------------
    C \ f |-> (E1, E2) : R

Instance 2:

    --------------------
    C U {E : C} \ E : C

Instance 3:

    C \ E1 : Powerset(E2)
    C \ E3 : E1
    --------------------
    C \ E3 : E2

## Hypothetical Deduction Calculus Rules for Context Formation

Instance 1 (U here is union):

    C \ D : SSet
    C \ R : SSet
    --------------------
    C U {D -> R : SSet} \

Instance 2:

    C \ exp : El
    C \ T :: Cls
    --------------------
    C U {exp : T} \

Instance 3 (where v is any syntactic variable):

    C \ T :: Cls
    --------------------
    C U {v : T} \

Instance 4 (where v is any syntactic variable):

    --------------------
    C U {v :: Cls} \

## Deduction Example

    C1 = { N : SSet, + : N * N -> N, * : N * N -> N, m : N, n : N, ...}

    C1 \ m : N

    C1 \ n : N

    C1 \ + : N * N -> N

    C1 \ m + n : N

    C1 \ * : N * N -> N

    C1 \ m * (m + n) : N

    |- C1 \ m * (m + n) : N

## Seeking a Typing Context Category Basis

- Hyperentity: the "universe" for first order theories


