## Task

As the title suggests, the task is to take high level RESOLVE code as typed by the user and produce an equivalent form in Java. This means finding a way to morph RESOLVE operations, expressions, and statements into a form that plays nice with Java, while still remaining 'true enough' to the organization and design of the original source.

The proceeding discussion requires a certain level of knowledge and familiarity with RESOLVE syntax and semantics. While I take the time to introduce certain things such as enhancements, this is done merely to set the stage for further discussion of translation. Thus, in general, I assume any discussion of the language itself is review for readers and thus omit many details. Finally, the code discussed in these notes can be found primarily in the compiler's [codegen package](https://github.com/Welchd1/resolve-lite/tree/master/src/main/java/resolvelite/codegen), and to some extent the [semantics package](https://github.com/Welchd1/resolve-lite/tree/master/src/main/java/resolvelite/semantics).

## Discussion

### Input Overview and Sample

The sorts of files our translator will be taking as input are all *executable* RESOLVE modules. In practice, this includes everything from facility modules to concept and enhancement modules. Just about the only thing that we don't want to produce code for are RESOLVE's mathematical precis modules. This makes the scheme for our source to source translator pretty straightforward: For each input module, we need to produce exactly one Java output class that faithfully models the original input/source module.

To give a flavor as to the type of code we'll be generating, the following is an abridged version of a stack concept specification
```
Concept Stack_Template(type T; 
        evaluates Max_Depth :
              Std_Integer_Fac :: Integer)
    uses Standard_Integers,
         Integer_Theory

    requires Max_Depth > 0
            which entails Max_Capacity : N

    Type family Stack is
            modeled by Str(T)
        exemplar S
        constraint |S| < Max_Capacity
        initialization
            ensures S = Empty_String

    Operation Push (updates S : Store;
                   restores e : T)
        requires |S| < Max_Depth
        ensures S = <@e> o @S
    ...
end Stack_Template
```

Here is what is produced when the above is translated to Java:
```java
import edu.clemson.resolve.runtime.*;
import java.lang.reflect.*;

public interface Stack_Template {
    interface Stack extends RType { }
    public RType initStack();
    public void Push(RType S, RType e);
    public RType getT();
    public RType getMax_Depth();
}
```

One thing to notice is that for simplicity -- whether it is needed or not -- we uniformly import `java.lang.reflect.*` as this library is needed for translation of enhancement module implementations. The other library, `edu.clemson.resolve.runtime.*`, contains a small collection of base and utility classes needed by the Java code we generate.

Note that all resolve module's we'll be translating grammatically share a similar structure:
```
grammar Resolve;

resolveFile
    :    [moduleType_1] | ... | [moduleType_n]
    ;

[moduleType_i]
    :    [Header]';'
         [preamble] (requires, uses, etc)
         [body] (zero or more declarations)
         'end' ';'
    ;
```

##Relevant Constructs: Translation to Java

One thing to note when looking at the example above is that the specification portions within our modules (e.g. the `requires`, `ensures` clauses) are not included -- as they have no executable meaning. So the only things we really need to worry about are program expressions, statements, and declarations, where relevant declarations include:
* type models
* type representations
* functions (operation decls, operation impls)
* facilities

While the real meat of the translator's job is in correctly translating the aformentioned declarations, *there are* are some notable complexities in forming 'Java correct' member access expressions such as `a.b.c` -- which I'll briefly discuss.

The rest of this section is dedicated to a more detailed discussion of individual constructs and their translation.

###Types

Every type in RESOLVE from the 'primitive' ones such as `Boolean` and `Integer` to `Stack`s, `Queue`s and `List`s are introduced via a mathematical model found within a concept or enhancement. These models, in order to be used in any programmatic capacity, must first be given a concrete, programmatic representation inside an implementation module. Each of these constructs (introduced in separate modules) carries a distinct Java translation.

#####Models

Type models such as the `Type Family Stack is modeled by...` portion of `Bdd_Stack_Template` above simply translates down to the following interface that will always be nested in a translated concept or enhancement class:
```java
interface Stack extends RType { }
```
Here, and everywhere else it appears, the predefined type object `RType` can be roughly equated to "the type of all translated resolve program types". That is, anywhere a type appears as part of a declaration, we simply mask whatever the original reference was by `RType`, which we can safely do as it's assumed a previous phase of compilation has already assigned and checked program types.

#####Representations

Given our interface-based translation of a type model, one can then think of the translation of a RESOLVE type representation as the implementation of some type model interface. For example, a representation of the `Stack` model in RESOLVE might look like the following:

```
Type Stack = Record
        Contents : Array_Fac :: Ceramic_Array
        Top : Std_Integer_Fac :: Integer
    end;
    convention 0 <= S.Top and
        S.Top <= Max_Depth
    correspondence
        conc.S = Reverse(
            Iterated_Concatenation(1, S.Top,
                    lambda(i : Z).(
                        <S.Contents(i)>)))
```

And in Java:

```java
class Stack implements RType {
    Stack_Rep rep;
    Stack() {
        rep = new Stack_Rep(this);
        rep.initialize(this);
    }

    @Override public Object getRep() { return rep; }

    @Override public void setRep(Object o) { rep = (Stack_Rep)o; }

    @Override public RType initialValue() { return new Stack(); }

    @Override public String toString() { return rep.toString(); }
}
class Stack_Rep {
    RType Contents;
    RType Top;
    Stack_Rep(Stack e) {
        this.Contents = ((Bdd_Ceramic_Array_Template)Arr_Fac)
                               .initCeramic_Array();
        this.Top = ((Integer_Template)Standard_Integers.INSTANCE)
                               .initInteger();
    }

    private void initialize(Stack S) { }

    @Override public String toString() {
        return Contents.toString()+Top.toString();
    }

    public RType initStack() { return new Stack(); }
    public RType getStack() { return initStack(); }

    public Stk_Array_Impl(RType T, RType Max_Depth) {
        this.T = T;
        this.Max_Depth = Max_Depth;
        this.Arr_Fac =
            new Ceramic_Array_Impl(Bdd_Stack_Template.getT(),
         ((Integer_Template)Standard_Integers.INSTANCE).initInteger(1),
         getMax_Depth());
    }
}
```
Notice that a large bulk of the code included above is fairly boilerplate as it consists mainly of accessor methods and initialization assignments for fields contained within the aggregate (record) type `Stack`.

While on the topic of type representation, its worth mentioning that RESOLVE also supports the use of 'model-less representation types' which, like the name suggests, are simply representations lacking a concept (or enhancement) defined type model. A trivial example of such a type is the following:

```
Facility Cartesian_Grid_Fac
        uses Standard_Integers

    Type Point = Record
            x, y : Std_Integer_Fac :: Integer
        end
        convention
            0 < x, y <= 50
        initialization
            x := 2
            y := 3
        end

end Cartesian_Grid_Fac
```

The translated output of these remains unchanged, though in this case notice that we've included an `initialization` block in which we assign starting values of 2 and 3 to fields `x` and `y`, respectively. Thus, our translator would additionally need to translate these statements and place them within the
```java
void initialize(Point P) { }
```
method of our generated `Point_Rep` class.

### Operations

Translation of operations is made a great deal simpler by draping all specific type references with a general reference to `RType`. For example, translation of `Push` from `Bdd_Stack_Template` becomes:
```java
public void Push(RType e, RType S);
```
Since there's no return type on `Push`, this simply translates to a Java's `void` type.

### Facilities

Facilities are by far the trickiest declarations to translate simply because there isn't a straightforward Java equivalent of the functionality layering capabilities that enhancements provide.

Before we get into enhancements, let's start by considering unenhanced facility declarations:

```
Facility SF is Bdd_Stack_Template
            <Std_Integer_Fac :: Integer>(5)
        implemented by Stk_Array_Impl
```
Here we pair a specification (`Bdd_Stack_Template`) with a corresponding implementation (`Stk_Array_Impl`) allowing users to create `Stack`s as follows: `Var S : SF :: Stack`. Here is the declaration of `SF` in Java:

```Java
Bdd_Stack_Template SF = new Stk_Array_Impl(/*'Integer'*/, /*'5'*/);
```
and its usage in a variable declaration:

```Java
RType S = ((Bdd_Stack_Template)SF).initStack();
```
Clearly the facility from which a fresh `Stack` is obtained is important since each facility declaration can be parameterized by any number of specific values. For example, facility `SF` produces `Stack`s of maximum length 5 of type `Integer`. Further, it's best not to think of the type object `Stack` providing access to available operations, but rather, the facility `SF` itself. For instance in RESOLVE we make the call `SF :: Push(S, x)` not `S.Push(x)`. Thus for an `SF`-qualified call to `Push`, the `S` argument had better be a `Stack` drawn from `SF`.

#####Standard Facilities
Moving away from `Stack`s for the moment, this brings us to retrieval and translation of 'primitive' types such as `Boolean`s, `Integer`s, `Character`s, and `Character_Str`s. As expected, there are facilities pairing specifications and implementations for each of these core notions, though it's important to realize that only a single facility declaration should perform this task (for each). That is, every time a user declares a new variable of type `Integer` the following 'standard' short facility should be referenced:

```
Facility Std_Integer_Fac is Integer_Template
    externally implemented by Integer_Impl;
```
So in translated code we don't want to create new instances of `Integer_Impl` for every translated file. Instead, we use the [singleton design pattern](https://en.wikipedia.org/wiki/Singleton_pattern) to ensure that fresh `Integer`s are obtained through *the* `Std_Integer_Fac` facility (which accurately models the situation within the source). Here is the Java translation of the short facility module that houses our `Integer` facility singleton:
```Java
public class Std_Integer_Fac {
    private Std_Integer_Fac() { }
    public static final Integer_Template INSTANCE = new Integer_Impl();
}
```
and below a comparison of its declaration in RESOLVE and Java.

<table border="0">
<tr><th><b>RESOLVE</b></th><th><b>Java</b></th></tr>
<tr>
<td><pre>
Var Max : Std_Integer_Fac :: Integer;
</td>
<td>
<pre>
RType Max = ((Integer_Template)Standard_Integers
         .INSTANCE).initInteger()
</td>
</tr>
</table>

####Dealing with facility arguments

Since facilities are RESOLVE's mechanism of specializing modules with actual values, our translation also needs to account for the various forms these arguments can take. I consider there to be (roughly) four different sorts of arguments:

##### 1. Types

Translation of type arguments follows pretty much exactly the same as it does in the context of a variable declaration. For instance, in `Facility SF is Bdd_Stack_Template<Std_Integer_Fac :: Integer>(5) ...` the integer type between the angled braces simply translates to an initialization of `Integer`, which is consequently same thing we'd do in the context of a variable decl:
```
((Integer_Template)Standard_Integers.INSTANCE).initInteger()
```
However, say we're in the context of a concept implementation module and we choose to specialize a facility declaration with a generic parameterizing the concept we're implementing. Even in this case, we translate the argument the same as we would in the context of variable decl. For instance, for a generic variable `e : T` we say `RType e = T`, so the translation here for the argument would just be `T`. Note that for implementation concerns we occasionally will wrap a reference to `T` in getter or initter method called `getT()` or `initT`, though each of these does what we just discussed -- return `T`.

##### 2. Constants

Handling of constant arguments follows very closely to argument initialization for non-generic types (like `Integer`, `Character`, etc). In fact, it's pretty much the same except we're providing a constant initial value, for example:
```
((Integer_Template)Standard_Integers.INSTANCE).initInteger(5)
```

##### 3. Named variables

I take 'named' to simply mean variables that reference some prior declared symbol -- *excluding* operations. For instance `Max_Depth` in `Bdd_Stack_Template` I would consider a named variable. Translation of these is pretty simple and follows closely translation of generic arguments (e.g. ends up being `getMax_Depth()`, etc).

##### 3. Operations

Arguments referencing operations are by far the most difficult to translate. First, in order to do so, we require a certain level of semantic information. For instance, say we see the following facility: `Facility F is T_I(Foo, x, y, z) ...`. What argument here is referencing an operation, if any? Compounding this difficulty more than the lookups required to figure this out reliably is the fact that Java does not natively permit passing operations as parameters (ignoring for the moment some new features in Java 8). Conventionally, to get around this, Java programmers simply wrap the formal parameter in an interface, and, in the context of specialization, create an anonymous implementation of the interface implementing (or just calling) the desired method.

Without going into too many details, this turns out to be pretty tough to do as the (old) translator needed to find not only the operation corresponding to the actual argument, but also the formal operation parameterizing whatever module we're specializing. Even with the many updates to our compiler's symboltable machinery, finding the formals corresponding to a given actual is still a pain to do correctly as there is currently no standard mechanism written for doing so. Instead of doing this, we use the fact that everything has already been typechecked to abstract the notion of an operation parameter into a general interface that can just be defined in the runtime (see `OperationParameter` and `BaseOperationParameter`). In a nutshell, this saves us from needing to know the name and number of parameter for the formal operation parameter.

####Enhancements

Facility translation is complicated by the addition of *enhancements*. For instance, we can enhance the aforementioned `SF` facility with basic a 'no-oping' capability by doing the following:

```
/** An enhancement for {@code Bdd_Stack_Template::Stack}s that pushes
 *  and pops  an entry from {@code S}, accomplishing nothing.
 */
Enhancement Do_Nothing_Capability for Bdd_Stack_Template;
        Oper Do_Nothing (restores S : Stack);
end Do_Nothing_Capability;

Facility SF is Bdd_Stack_Template
        <Std_Integer_Fac :: Integer>(5)
    implemented by Stk_Array_Impl
        enhanced by Do_Nothing_Capability
    implemented by Stk_Do_Nothing_Impl;
```
An unenhanced `SF` provides access to the following operations: `Push`, `Pop`, and the other operations declared in `Bdd_Stack_Template`, while the enhanced facility includes all previously mentioned operations, plus `Do_Nothing`. Before discussing how we're going to handle translation of the enhanced `SF` above, here is the Java we produce for the `Do_Nothing_Capability` enhancement specification:

```java
public interface Do_Nothing_Capability extends Bdd_Stack_Template {
    public void Do_Nothing(RType S);
}
```
... and its implementation, `Stk_Do_Nothing_Impl`:
```java
public class Stk_Do_Nothing_Impl
        implements
            Do_Nothing_Capability, Bdd_Stack_Template {
    ...
}
```

Unsurprisingly, our translation of the `Do_Nothing_Capability` specification closely mirrors the way we translate concept specifications: as an interface housing primary (or, in this case, secondary) operation signatures. However, the key distinction here is that our enhancement interface `extends Bdd_Stack_Template`, effectively giving anything of Java type `Do_Nothing_Capability` access to *all* available operations (including `Do_Nothing`).

In thinking then about how to best handle translation of enhanced facility declarations, it's tempting to turn to the [decorator design pattern](https://en.wikipedia.org/wiki/Decorator_pattern), which, according to Wikipedia:
".. allows behavior to be added to an individual object, either statically or dynamically, without affecting the behavior of other objects from the same class".

So since all we want to do is extend our base concept, `Bdd_Stack_Template` with the additional `Do_Nothing` operation, the following 'decorated' instantiation seems like a good place to start:

```java
Bdd_Stack_Template SF = new Stk_Do_Nothing(new Stk_Array_Impl(..))
 ```

with this translation, a call such as `Do_Nothing(S)` would get translated and qualified as follows:

```java
((Do_Nothing_Capability)SF).Do_Nothing(S)
 ```

####Enhancements: the nitty gritty
You might be wondering -- after reading the above -- *why* the hell we're translating facility declarations as `<root concept> <name> = new ...` when we could make our lives easier if we simply took the specification of the bottommost enhancement (if there indeed such an enhancement), and made that the `<root concept>`.

For example, if the above declaration of `SF` simply became `Do_Nothing_Capability SF = new Stk_Do_Nothing(..)` the call would take on a simpler, cast-less form that looks like the following: `SF.Do_Nothing(S)`. You'll note that this logic applies also to the zero enhancement case where `<root concept>` trivially ends up in the correct place -- yet even here you'll notice we still chose to present the more verbose, complicated translation that includes unnecessary casts.  The reason for this is explained and (hopefully!) justified in the next section, wherein we discuss translation of facilities with *n* enhancements. Indeed, up until this point, I've omitted the important detail that there can be an arbitrary number of parameterized enhancements. So the situation grammatically is actually better expressed through the following (work in progress) rules:

```
facilityDecl
    :    'Facility' ID 'is' ID ('<' type (',' type)* '>')?
         (moduleArgumentList)? 'implemented' 'by' ID
         (moduleArgumentList)? (enhancementPair)* ';'
    ;

enhancementPair
    :    'enhanced' 'by' ID ('<' type (',' type)* '>')?
         (moduleArgumentList)? 'implemented' 'by' ID
         (moduleArgumentList)?
    ;
```
|*Optional: not quite there yet|
|-------------|
|*The grammar rule* `enhancementPair` *presented above is insufficient as it erroneously assumes that each enhancement will always have a unique implementation.* For example, `Bdd_Stack_Template` might have an enhancement `Get_Nth_Ability`, which replicates and returns the Nth element from the top of the stack. There may be an `Obvious_Get_Nth_Impl` that does what you'd expect: repeatedly pop some elements off the top to get to the requested element, replicate it, then push everything back on. Using this enhancement would be a fine choice for extending `Bdd_Stack_Template` as implemented by `Pointer_Impl`. However, if we happen to know that we're using `Array_Based_Impl`, we can do much better. For this purpose, `Array_Based_Impl` can directly incorporate enhancements and provide a direct realization that takes advantage of implementation details.  I.e., `Get_Nth()` would be included as a procedure inside `Array_Based_Impl`. In such a case, we would declare a facility like this: `Facility Indexible_Stack is Bdd_Stack_Template enhanced by Get_Nth_Ability implemented by Array_Based_Impl;` thus foregoing the need to provide a specific corresponding implementation of `Get_Nth_Ability` (as it's rolled into `Array_Based_Impl`).|

##### *n*-enhancement translation

Let's see how well our decorator idea from before fares in the case of facilities with *two or more* enhancements:

    Facility SF is Bdd_Stack_Template<Integer>(4)
            implemented by Stk_Array_Impl
    	enhanced by Reading_Capability
                implemented by Obvious_Reading_Impl(Std_Integer_Fac :: Read)
        enhanced by Writing_Capability
                implemented by Obvious_Writing_Impl(Std_Integer_Fac :: Write);

Ignoring for the moment the arguments parameterizing these modules, as per our earlier example with one enhancement, we end up with something that looks like:

```java
Bdd_Stack_Template SF = new Obvious_Writing_Impl(..,
         new Obvious_Reading_Impl(.., new Stk_Array_Impl(..)))
```

DISCUSS AND WRAP UP (then section on tricky expressions -- member accesses, etc)

##Implementation

A working implementation of the code generator discussed in these notes can be found within the [codegen](https://github.com/Welchd1/resolve-lite/tree/master/compiler/src/edu/clemson/resolve/codegen) package of the RESOLVE-lite compiler.

The present implementation uses an automatically generated [ANTLRv4](http://www.antlr.org/) sax-dom style listener to traverse the parse tree representing input, producing an *output model object* hierarchy. After defining [StringTemplates (STs)](http://www.stringtemplate.org/) dictating how each model node will look in Java, we then automatically convert our built model hierarchy to a corresponding ST hierarchy, which is ultimately rendered and written as output.

At its core, this is a [model view controller (MVC)](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller) driven approach to codegen that emphasizes a strict separation between inputs and outputs. In this case, the delineation between the model, view, and controller portions is fairly clear: Our output model hierarchy clearly represents the 'model' portion, while the controller and view portions fall squarely on the parse tree listener (which dictates how our output model is constructed) and the templates, respectively. It's worth noting that this is roughly the same strategy employed by ANTLR itself to produce the code that drives the state machines for a given grammar.

The remaining sections delve into more detail as to how this all working. In fact, much of the proceeding documentation, while tweaked to resemble RESOLVE, follows directly from [Terrance Parr's](http://parrt.cs.usfca.edu/)  various lecture and course materials.

###Input

The `Cartesian_Grid_Fac` facility -- which appears above in the 'Types' section of this document -- when run through ANTLR produces the following parse tree:

![translation input](https://github.com/Welchd1/resolve-lite/blob/master/doc/images/translationinput.png)

You'll notice this representation of the tree has been overlaid with information about `PTType`s (program types) and scopes for clarity. It's the phase of compilation preceding codegen that is responsible for building the scopes delineated above, filling them, and annotating all expression nodes and references within the tree with appropriate `PTType` (program-type) information.

More information on RESOLVEs current approach to symbol table construction and entry population should be forthcoming in a separate document that I hope (read: will eventually!) link to here.

###Constructing an Output Model

In order to generate code from the representation shown above, we once again walk the parse tree. But, instead of printing or buffering text output directly, we instead create model objects because it is a much more flexible mechanism. The order we create these objects and hook them together is irrelevant. We don't generate output until the entire model has been created. Consequently, we can follow the order of the input by walking the input parse tree to construct the model. The model objects I create represent the important elements of our desired Java output: namely definitions (defs), statements (stats), and exprs (expressions):

![output model objects](https://github.com/Welchd1/resolve-lite/blob/master/doc/images/models.png)

In the [`Java.stg`](https://github.com/Welchd1/resolve-lite/blob/master/compiler/resources/edu/clemson/resolve/templates/codegen/Java.stg) template group file, you will see templates that correspond by name with the model objects:

```
ParameterDef(param) ::= ""

VariableDef(var, init) ::= ""

FunctionDef(func, params) ::= << ... >>

FunctionImpl(func, params, vars, stats) ::= << ...  >>

ModuleFile(file, module) ::= << ... >>
...
```
###Generating Java code from the model

The [`ModelConverter`](https://github.com/Welchd1/resolve-lite/blob/master/compiler/src/edu/clemson/resolve/codegen/ModelConverter.java) is an automated tool written by Terrance Parr that automatically converts an output model tree full of `OutputModelObject`s to a template hierarchy by walking the output model (depth-first). Each model object has a corresponding template of the same name. An output model object can have nested objects by annotating its various fields with `@ModelElement` annotations. Upon seeing this annotation, the model converter will automatically instantiate templates for these fields and add them as attributes to the template for the current `OutputModelObject`, thus 'filling in' the current target template. For example, here is the root of RESOLVE output model hierarchy:

```java
public class ModuleFile extends OutputModelObject {
    public String RESOLVEVersion;
    public String resolveFileName;
    @ModelElement public Module module;

    public ModuleFile(String resolveFileName) {
        this.resolveFileName = resolveFileName;
        this.RESOLVEVersion = RESOLVECompiler.VERSION;
    }
}
```

The model converter assumes that the corresponding template's first parameter is for this `OutputModelObject` object, followed by the names of any nested model elements. In this case, the corresponding template looks like this:

```
ModuleFile(file, module) ::= <<
    <fileHeader(file.resolveFileName, file.RESOLVEVersion)>
    import java.lang.reflect.*;

    <module>
>>
```
The first object, however it's named within the template, is always set to the model object associated with the template.

|Confusion point|
|-------------|
|*The parameters associated with the nested model objects are templates not output model objects!* For example, the `file` template parameter will contain an instance of the Java class `ModuleFile` but `module` (the next parameter over) will contain an instance of a **template** -- specifically a template named `Concept`, `ConceptImplModule`, `FacilityImplModule`, or any other subclass of the abstract `Module` class. The point to realize is that it won't be the Java object, but the template representing them.|

####An example from the compiler

Let's work though an example involving usage of the `ModuleFile` template. Below is a image of the template definition exactly as it appears in the compiler's `Java.stg` file, complete with line numbers:

![output model objects]()

1. Template `ModuleFile` (line 32) takes two parameters, the first of which (`file`) refers to the model object itself. This is injected by the `ModelConverter`. The second parameter `module`, is automatically injected by the model converter as well. It is a template created for the corresponding field in the `ModuleFile` Java class:

 ```java
@ModelElement public Module module;
```

The annotation indicates to the `ModelConverter` that it should create a template associated with the model object referenced by that field, and inject the resulting template as a parameter to the template associated with the enclosing object (a `ModelFile` object). [`Module`](https://github.com/Welchd1/resolve-lite/blob/master/compiler/src/edu/clemson/resolve/codegen/model/Module.java), the type of the `module` field, references an *abstract* model object, whose concrete subclasses each have corresponding templates. For example, `Concept` is model object subclassing `Module`, and here is its corresponding template:
```
Concept(concept, types, funcs) ::= <<
public interface <concept.name> {
    <types; separator="\n">
    <funcs; separator="\n">
} >>
```
Similarly, the `Concept` Java model object has two annotated fields named: `types` and `funcs`, which, like the `module` field discussed above, will automatically be injected by the `ModelConverter`.

2. Line 33 references another template called `fileHeader`, which takes as arguments certain fields from the `file` model object (such as `<file.resolveFileName>`) and the header at the top of each generated file. Keep in mind that in order for StringTemplate to access the field of an output model object, the field in question *must* be declared `public`.

We don't go through the trouble of creating a specialized model object for this, since it can produce the necessary info by just referencing strings already present in the `file` model object:
```
fileHeader(resolveFileName, RESOLVEVersion) ::= <<
/**
 * Generated from <resolveFileName> by RESOLVE version <RESOLVEVersion>.
 * This file should not be modified.
 */ >>
```
1. Line 34 is just some raw text that is to be emitted.
2. Line 36 states that the `module` template that was automatically injected should be emitted.
