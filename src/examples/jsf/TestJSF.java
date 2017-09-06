package jsf;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;

public class TestJSF {

	boolean test() {
		String[] args = new String[0];
		Config conf = JPF.createConfig(args);
		conf.setProperty("site", "${jpf-symbc}../site.properties");
		conf.setProperty("classpath", "${jpf-symbc}/build/main;${jpf-symbc}/build/examples;${jpf-symbc}/build/examples");
		
		conf.setProperty("listener", "gov.nasa.jpf.symbc.heap.HeapSymbolicListener");
		conf.setProperty("symbolic.lazy", "true");
		
		conf.setProperty("target", "jsf.TestNode");
		conf.setProperty("symbolic.method", "jsf.TestNode.foo(sym)");
		
		JPF jpf = new JPF(conf);	

		boolean violate = true;
		try {
			jpf.run();
			violate = jpf.foundErrors();
		} catch (JPFConfigException cx) {
			System.out.println("JPFConfigException: ");
			cx.printStackTrace();
		} catch (JPFException jx) {
			System.out.println("JPFException: ");
			jx.printStackTrace();
		}
		return !violate;
	}

	public static void main(String[] args) {
		TestJSF q = new TestJSF();
		// long time = System.currentTimeMillis();
		q.test();
		// System.out.println("Total elapsed time: " + (System.currentTimeMillis() - time) + " ms");
	}

}
