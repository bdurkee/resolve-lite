## Thoughts on RESOLVE `uses` clauses and project organization

These are some rough notes on resolve imports (`uses`) clauses.

### first: some high level proselytizing
Uses clauses in RESOLVE should be *simple*, **dead** *simple*. Towards this end, they should flow naturally from what
users typically will want to do while not getting in their way. For example, if you want to access exported
declarations from modules `X` and `Y`, then simply type:

```
uses X, Y
```
The root of this drive for simplicity is due in no small part to the fact that
we are already asking a fair bit of our hypothetical userbase: and would ideally like them to save most of their time
(not to mention brainpower and effort) towards understanding existing concepts, and engineering
formal contracts around these--much less verifying their implementations.

With this being said, what follows are several 'shoulds' and 'shouldn'ts' with regards to
a prospective `uses` system:

(*note: this list only contains my thoughts, and as such is likely far from complete--so add to it at will..*)

1. **should** be tailored specifically around the design and organizational characteristics of RESOLVE.
In other words, none of this: 'lets just rip out Java's package system out and use it verbatim.' We want to encourage
(enforce?) a *concept-based* organizational structure. Meaning any unit housing some reusable component
should be logically organized in the following manner (e.g. preferably around some concept):
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
2.  **should** be designed around referencing *modules*; as opposed to say, directories or packages.

3. **should** allow one to easily name and use modules in other (separate) reusable component libraries.

4. **should** encourage (require) users to structure their libraries in a disciplined manner (see 1).

5. **shouldn't** require programmers to type out long winded paths that are arbitrarily deep and organized in differing
ad hoc ways. For instance, requiring users to type the exact path to their modules (or directories) in a java manner
(`import foo.bar.biz.test.*`) or like golang (`../baz/cat/dog`)


### Current thinking: several variants

#### variant 1:

```
Concept T;
    uses (
        from dtwelch X, Y, Z;
        Standard_Booleans, Standard_Integers;
    );
    ...
end T;
```

For reference (and I'll eventually write a more comprehensive doc on this), say I have two environmental variables:
* **RESOLVEPATH** = `~/Documents/resolvework/`
* **RESOLVEROOT** = `/usr/local/lib/resolve/`

In the example above (and others of a similar flavor), the module `T` reads from a module library (e.g. directory)
named dtwelch--which is an immediate subdirectory of `RESOLVEPATH`
(e.g. $RESOLVEPATH/dtwelch/)-- and 'pulls in' modules `X`, `Y`, and `Z`.

Here, `Standard_Booleans` and `Standard_Integers` do not have a `from` clause preceeding them, indicating they are
somewhere within the `RESOLVEROOT` standard directory that are packaged with the compiler jars. These can be thought
of therefore as having an implicit `from` clause of `RESOLVEROOT`.

we can also say (of course):

```
Concept T;
    uses from cpsc372, Y, Z;
    /* or, alternatively */
    uses (
        from msitara X, Y;
        Standard_Booleans, Integer_Theory;
    );
end T;
```

Here the module names proceeding the `from` clause had better actually be in that library,
otherwise it should be an error.

So if I say `uses from dtwelch, Standard_Booleans`, even though `Standard_Booleans` would indeed be present
in the standard (std) library, the fact that it's not present in `dtwelch` makes this an error.

#### standard facility handling

Speaking of standard facilities, users will typically want to have easy access to these. The longhand way of doing this
has traditionally (in my system at least) looked something like this:
```
uses Standard_Booleans, Standard_Integers, Standard_Characters, Standard_Char_Strings;
```
the following shorthand would considerably shorten this though: `uses Std_Facs;` where `Std_Facs` would be a recognized
keyword that informs the compiler (and plugin) to use the 'normal' facilities located in `RESOLVEROOT`. Note that this
would not happen *automatically* behind the scenes:

1. Regardless of what people are saying, its good to have control over
which 'standard' things are imported (no matter how 'standard' they seem).
2. This would also allow us to avoid having the compiler tiptoe around accidentally including this 'automatic' logic
 in the standard facilities themselves (which would requiring hardcoding their names into the compiler, etc. By keeping
 the handling of this on the client end of things (albeit in a simplified form), we dodge needing to deal with this.
 Hell, using live templates provided by the plugin, one could just make the dam `uses Std_Facs` part a template
 that automatically gets inserted everytime you create a new file.

So overall the `uses Std_Facilities` seems like a reasonable compromise. Indeed, if I don't want any imported,
I could simply leave `Std_Facs` out of my `uses` list; and if I want only a subset of them, I simply resort to
explicitly naming them as we have until now.

#### variant 2

The second variant basically just a syntactic tweak of the first:

```
Concept T;
    uses X, Y from msitara;
    /* or */
    uses (
        X, Y from msitara;
        Standard_Booleans, Integer_Theory;
    );
end T;
```
I personally like the way this reads much better, the only problem is that it makes for a somewhat 'backwards' ordering.
For instance, I'm saying that I want to use `X` and `Y` -- BUT only from `msitara`! Consider the IDE perspective as
well: when the user finishes typing `uses ..` they'll get completions for all available modules -- namely those from
 the standard library -- but this will be before the
`from` clause is added, so their selections risk ultimately being rendered meaningless after the fact
(depending on their choice of `from` -- recall things outside of msitara shouldn't be valid here)..

So in summary, while I like the way it looks quite a bit better, the ordering for analysis makes it a tricky proposition
 on the IDE/tooling end of things.

#### variant 3

You might've noted in the previous variants that I was clearly trying to avoid having multiple `uses` clauses, though
given the ordering woes (as per variant 2), here's yet another alternative to solves this (but takes multiple clauses):

```
Concept T;
    from msitara uses X, Y;
    from dtwelch uses Spiral_Template;
    uses Standard_Booleans, Standard_Integers, Nat_Num_Th;
end T
```
I don't like it personally: it just seems like there's some much going on.. Though perhaps at a ceratain point that's
 going to be unavoidable :)

#### coming to terms with variant 1

```
uses from cpsc372, X, Y;
```
One of the real problems with variant 1 is the fact that the library name (in this case `cpsc372` seems to blur together
with `X` and `Y`.. Part of me wants to distinguish it however possible, for instance:

```
uses from "cpsc372" X, Y;
```

but this then is getting dangerously close to this sort of thing... :

```
uses from "../cpsc372/foo/" X, Y;
```
which really isn't what I'm looking for. Dunno. Maybe I'll mess with live templates some and see how close I can get
completions working well for variant 2.