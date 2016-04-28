## Thoughts on RESOLVE `uses` clauses and project organization

### some high level proselytizing
Uses clauses in RESOLVE should be *simple*, **dead** *simple*. Towards this end, they should flow naturally from what
users typically will want to do while not getting in their way (e.g. you want to access things
from a module `X`? Then type `uses X`). The root of this drive for simplicity is due in no small part to the fact that
we are already asking a lot of our hypothetical users: and would ideally like them to save most of their time
(not to mention brainpower and effort) towards understanding existing concepts available, and engineering
formal contracts around these--much less hopefully verifying any resultant implementation-level code.

With this being said, what follows are several 'shoulds' and 'shouldn'ts' with regards to
any prospective `uses` system:
(*note: this list only contains my thoughts, and as such is likely far from complete--so add to it at will..*)

(a) **should** be tailored specifically around the design and organizational characteristics of RESOLVE.
In other words, none of this: 'lets just rip out Java's package system out and use it verbatim.' We want to encourage
(enforce?) a *concept-based* organizational structure. Meaning any unit housing some reusable component
should be logically organized in the following manner:
```
Stack_Template.resolve
impls/
  .  Array_Impl.resolve
  .  Clean_Array_Impl.resolve
  .  LL_Impl.resolve
enhancements/
  .  Flipping_Capability.resolve
  .  impls/
  .  .  Iterative_Impl.resolve
  .  .  Recursive_Impl.resolve
*various precis/precis exts*
```
There is an emphasis
on interfaces and abstraction in the language Indeed, the organization of the code in those languages  Files and enclosing projects in our language are not the same or even organized around the same things.

* **should** be designed around referencing *modules*; as opposed to say, directories or packages.

* **should


It's clear given the design of the language and the high level of abstraction intended tha