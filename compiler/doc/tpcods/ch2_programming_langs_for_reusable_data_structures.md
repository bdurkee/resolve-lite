# 2 Programming Langugages for Reusable Data Structures

## Generic Facilities

A satisfactory programming language for data structures must at a minimum include an encapsulating construct that
allows implementers to change the realization chosen for a data structure without affecting the users of the structure.
We can illustrate how this might be accomplished by considering a facility for manipulating stacks of integers.

```python
Facility Custom_Integer_Stack_Fac;
    Type Stack = Record
        Contents : Array 1 .. 500 of Integer

```