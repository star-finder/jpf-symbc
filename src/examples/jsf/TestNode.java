package jsf;

/**
 *
 * @author Quoc-Sang Phan <sang.phan@sv.cmu.edu>
 *
 */
public class TestNode {

	public static Node foo(Node x)
	{
		if (x == null) return null; 
		else if (x.next == null) return x; 
		else return x.next;
	}
	
	public static void main(String[] args){
		Node x = new Node();
		foo(x);
	}
}
