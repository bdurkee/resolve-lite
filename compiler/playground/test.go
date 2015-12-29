package playground


type geometry interface {
	area() float64
	perim() float64
}
//For our example we’ll implement this interface on rect and circle types.
type rect struct {
	width, height float64
}



func foo(g geometry)	  {
	g.area();
}