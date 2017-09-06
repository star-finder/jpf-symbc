/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * Symbolic Pathfinder (jpf-symbc) is licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

//
//Copyright (C) 2006 United States Government as represented by the
//Administrator of the National Aeronautics and Space Administration
//(NASA).  All Rights Reserved.
//
//This software is distributed under the NASA Open Source Agreement
//(NOSA), version 1.3.  The NOSA has been approved by the Open Source
//Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
//directory tree for the complete NOSA document.
//
//THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
//KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
//LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
//SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
//A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
//THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
//DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//

package gov.nasa.jpf.symbc.numeric;

import gov.nasa.jpf.symbc.numeric.solvers.IncrementalListener;
import gov.nasa.jpf.symbc.numeric.solvers.IncrementalSolver;
import gov.nasa.jpf.symbc.numeric.solvers.ProblemGeneral;
import za.ac.sun.cs.green.Instance;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.arrays.ArrayConstraint;
import gov.nasa.jpf.symbc.arrays.RealArrayConstraint;
import gov.nasa.jpf.symbc.arrays.RealStoreExpression;
import gov.nasa.jpf.symbc.arrays.StoreExpression;
import gov.nasa.jpf.symbc.arrays.SelectExpression;
import gov.nasa.jpf.symbc.concolic.PCAnalyzer;
import gov.nasa.jpf.symbc.numeric.solvers.SolverTranslator;
import gov.nasa.jpf.symbc.numeric.visitors.CollectVariableVisitor;
import gov.nasa.jpf.symbc.string.StringPathCondition;
import gov.nasa.jpf.symbc.concolic.*;
import gov.nasa.jpf.symbc.arrays.ArrayExpression;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.VM;

import java.util.HashMap;
import java.util.Map;

// path condition contains mixed constraints of integers and reals

public class PathCondition implements Comparable<PathCondition> {
    public static boolean flagSolved = false;

    public HashMap<String, ArrayExpression> arrayExpressions;

    public Constraint header;
    int count = 0;
    protected int solverCalls = 0;

    private Instance instance = null;

    // TODO: to review
    public StringPathCondition spc = new StringPathCondition(this);

    private Integer hashCode = null;

    //added by guowei
    public static boolean isReplay = false;
    public static void setReplay(boolean isReplay){
		PathCondition.isReplay = isReplay;
	}

    public PathCondition() {
    	header = null;
        arrayExpressions = new HashMap<String, ArrayExpression>();
    }

    public Instance getInstance() {
		if (instance == null) {
			instance = SolverTranslator.createInstance(header);
		}
		return instance;
	}

    public int getSolverCalls(){
    	return this.solverCalls;
    }

	public PathCondition make_copy() {
		PathCondition pc_new = new PathCondition();
		pc_new.header = this.header;
	    pc_new.count = this.count;
	    pc_new.spc = this.spc.make_copy(pc_new); // TODO: to review
	    pc_new.solverCalls = this.solverCalls;
        pc_new.arrayExpressions = this.arrayExpressions;
		return pc_new;
	}

    //Added by Aymeric
    public void _addDet(Comparator c, SelectExpression se, IntegerExpression ie) {
        Constraint t;
        flagSolved = false;
        t  = new ArrayConstraint(se, c, ie);
        if(SymbolicInstructionFactory.incrementalSolving) {
          PCParser.addConstraint(t, (ProblemGeneral)IncrementalListener.solver);
        }

        prependUnlessRepeated(t);
    }

    //Added by Aymeric
    public void _addDet(Comparator c, StoreExpression se, ArrayExpression ae) {
        Constraint t;
        flagSolved = false;
        t  = new ArrayConstraint(se, c, ae);
      if(SymbolicInstructionFactory.incrementalSolving) {
        PCParser.addConstraint(t, (ProblemGeneral)IncrementalListener.solver);
      }
        prependUnlessRepeated(t);
    }

    //Added by Aymeric
    public void _addDet(Comparator c, SelectExpression se, RealExpression re) {
        Constraint t;
        flagSolved = false;
        t  = new RealArrayConstraint(se, c, re);
      if(SymbolicInstructionFactory.incrementalSolving) {
        PCParser.addConstraint(t, (ProblemGeneral)IncrementalListener.solver);
      }
        prependUnlessRepeated(t);
    }

    //Added by Aymeric
    public void _addDet(Comparator c, RealStoreExpression se, ArrayExpression ae) {
        Constraint t;
        flagSolved = false;
        t  = new RealArrayConstraint(se, c, ae);
      if(SymbolicInstructionFactory.incrementalSolving) {
        PCParser.addConstraint(t, (ProblemGeneral)IncrementalListener.solver);
      }
        prependUnlessRepeated(t);
    }

	//Added by Gideon
	public void _addDet (LogicalORLinearIntegerConstraints loic) {
		//throw new RuntimeException ("Not being used right now");
		if (!this.hasConstraint(loic)) {
			flagSolved = false;
			Constraint t = (Constraint) loic;
      if(SymbolicInstructionFactory.incrementalSolving) {
        PCParser.addConstraint(t, (ProblemGeneral)IncrementalListener.solver);
      }
			t.and = header;
			header = t;
			count++;
		}
	}

	public void _addDet(Comparator c, Expression l, Expression r) {
		if (l instanceof IntegerExpression && r instanceof IntegerExpression)
			_addDet(c,(IntegerExpression)l,(IntegerExpression)r);
		else if (l instanceof RealExpression && r instanceof RealExpression)
			_addDet(c,(RealExpression)l,(RealExpression)r);
		else
			throw new RuntimeException("## Error: _addDet (type incompatibility real/integer) " + c + " " + l + " " + r);

	}

	// constraints on integers
	public void _addDet(Comparator c, IntegerExpression l, long r) {
		flagSolved = false; // C
		_addDet(c, l, new IntegerConstant(r));
	}

	public void _addDet(Comparator c, long l, IntegerExpression r) {
		flagSolved = false; // C
		_addDet(c, new IntegerConstant(l), r);
	}

	public void _addDet(Comparator c, IntegerExpression l, IntegerExpression r) {

		Constraint t;
		flagSolved = false;
		if ((l instanceof LinearIntegerExpression) && (r instanceof LinearIntegerExpression)) {
			t = new LinearIntegerConstraint(l, c, r);
		} else {
			t = new NonLinearIntegerConstraint(l, c, r);
		}
    if(SymbolicInstructionFactory.incrementalSolving) {
      PCParser.addConstraint(t, (ProblemGeneral)IncrementalListener.solver);
    }
		prependUnlessRepeated(t);

	}


	// constraints on reals
	public void _addDet(Comparator c, RealExpression l, double r) {
		flagSolved = false; // C
		_addDet(c, l, new RealConstant(r));
	}

	public void _addDet(Comparator c, double l, RealExpression r) {
		flagSolved = false; // C
		_addDet(c, new RealConstant(l), r);
	}

	public void _addDet(Comparator c, RealExpression l, RealExpression r) {
		Constraint t;

		flagSolved = false; // C

		t = new RealConstraint(l, c, r);
    if(SymbolicInstructionFactory.incrementalSolving) {
      PCParser.addConstraint(t, (ProblemGeneral)IncrementalListener.solver);
    }
		prependUnlessRepeated(t);

	}

//	mixed real/integer constraints to handle cast bytecodes

	public void _addDet(Comparator c, RealExpression l, IntegerExpression r) {
		Constraint t;

		flagSolved = false; // C

		t = new MixedConstraint(l, c, r);
    if(SymbolicInstructionFactory.incrementalSolving) {
      PCParser.addConstraint(t, (ProblemGeneral)IncrementalListener.solver);
    }
		prependUnlessRepeated(t);

	}
	public void _addDet(Comparator c, IntegerExpression l, RealExpression r) {
		Constraint t;

		flagSolved = false; // C

		t = new MixedConstraint(r, c, l);
    if(SymbolicInstructionFactory.incrementalSolving) {
      PCParser.addConstraint(t, (ProblemGeneral)IncrementalListener.solver);
    }
		prependUnlessRepeated(t);

	}

   /**
     * Prepends the given constraint to this path condition, unless the constraint is already included
     * in this condition.
     *
     * Returns whether the condition was extended with the constraint.
     */
    public boolean prependUnlessRepeated(Constraint t) {
    	// if Green is used and slicing is on then we always add the constraint
    	// since we assume the last constraint added is always the header
        if ((SymbolicInstructionFactory.greenSolver != null)
        		|| !hasConstraint(t)) {
            t.and = header;
            header = t;
            count++;
            // remember that the most recently added constraint is not a string one
    	    spc.setRecentlyAddedConstraintNumeric();
            return true;
        } else {
            return false;
        }
    }

    public void prependAllConjuncts(Constraint t) {
       t.last().and = header;
       header = t;
       count= length(header);
    }

    public void appendAllConjuncts(Constraint t) {
        Constraint tmp = header.last();
        tmp.and = t;
        count= length(header);
     }

     public void appendPathcondition(PathCondition pc) {
        while (pc.header != null) {
            // Since we are only using it to append heapPC to pc, it may not be required to use prependUnlessRepeated
            prependUnlessRepeated(pc.header.copy());
            pc.header = pc.header.and;
        }
     }

    private static int length(Constraint c) {
        int x= 0;
        while (c != null) {
            x++;
            c = c.getTail();
        }
        return x;
    }

    /**
     * Returns the number of constraints in this path condition.
     */
	public int count() {
		return count;
	}

	/**
	 * Returns whether this path condition contains the constraint.
	 */
	public boolean hasConstraint(Constraint c) {
		Constraint t = header;

		while (t != null) {
			if (c.equals(t)) {
				return true;
			}

			t = t.and;
		}

		return false;
	}

	public Constraint last() {
		Constraint t = header;
		Constraint last = null;
		while (t != null) {
			last = t;
			t = t.and;
		}

		return last;
	}

	public boolean solve() {
		if (SymbolicInstructionFactory.greenSolver == null)
			return solveOld();
		else 
			return solveGreen();			
	}
	
	public boolean simplify() {
		if (SymbolicInstructionFactory.greenSolver == null)
			return simplifyOld();
		else 
			return simplifyGreen();
	}
	
	public Map<String, Object> solveWithValuation() {
		SymbolicConstraintsGeneral solver = new SymbolicConstraintsGeneral();

		Map<String,Object> result1 = solver.solveWithSolution(this);
		solver.cleanup();
		PathCondition.flagSolved = true;
		return result1;
	}

	private boolean solveWithSolution() {
		//
		// PEND: Why does this if check for instance==null?
		//       Why does the else NOT check that condition? (Looks fishy)
		//       What do we know about the prior value of this.instance here?
		//

		if (instance == null && spc.header == null) {
			System.out.println("\nCalling Green on Constraint (header):" +
				("\n" + header.toString()).replaceAll("\n", "\n        ")
			);
			//System.out.println("  ********** KNOWN: " + spc.isRecentlyAddedConstraintKnown() + " ***** NUM: " + (spc.isRecentlyAddedConstraintKnown() ? spc.isRecentlyAddedConstraintNumeric() : "?") + " ***** STR: " + (spc.isRecentlyAddedConstraintKnown() ? spc.isRecentlyAddedConstraintString() : "?") + " **********");
			instance = SolverTranslator.createInstance(header);
		}
		else if (header == null){
			System.out.println("\nCalling Green on StringConstraint (spc.header):" +
					("\n" + spc.header.toString()).replaceAll("\n", "\n        ")
				);
			//System.out.println("  ********** KNOWN: " + spc.isRecentlyAddedConstraintKnown() + " ***** NUM: " + (spc.isRecentlyAddedConstraintKnown() ? spc.isRecentlyAddedConstraintNumeric() : "?") + " ***** STR: " + (spc.isRecentlyAddedConstraintKnown() ? spc.isRecentlyAddedConstraintString() : "?") + " **********");
			instance = SolverTranslator.createStringInstance(spc.header);
		}
		else{
			// Handle mixed constraint
			System.out.println("\nCalling Green on both a Constraint and a StringConstraint (mixed constraint!)");
			System.out.println("Constraint (header):" +
					("\n" + header.toString()).replaceAll("\n", "\n        ")
				);
			System.out.println("StringConstraint (spc.header):" +
					("\n" + spc.header.toString()).replaceAll("\n", "\n        ")
				);
			//System.out.println("  ********** KNOWN: " + spc.isRecentlyAddedConstraintKnown() + " ***** NUM: " + (spc.isRecentlyAddedConstraintKnown() ? spc.isRecentlyAddedConstraintNumeric() : "?") + " ***** STR: " + (spc.isRecentlyAddedConstraintKnown() ? spc.isRecentlyAddedConstraintString() : "?") + " **********");
			if(! spc.isRecentlyAddedConstraintKnown()) {
				throw new RuntimeException("Expected recently added constraint (num vs str) to be known at this point!");
			}
			
			// Translate both constraints to Instances
			Instance instance_num = SolverTranslator.createInstance(header);
			Instance instance_str = SolverTranslator.createStringInstance(spc.header);
			
			// Merge them into a single Instance, making sure to put the one with the
			// recently added constraint on the left side.
			if(! spc.isRecentlyAddedConstraintKnown()) {
				throw new RuntimeException("We would expect this to be known at this point! What happened?");
			}
			if(spc.isRecentlyAddedConstraintNumeric()) {
				instance = Instance.merge(instance_num, instance_str);
			} else {
				instance = Instance.merge(instance_str, instance_num);
			}
		}
		boolean isSat = (Boolean) instance.request("sat");  /*&& spc.simplify()*/; // strings are not supported by Green for now

		/*
		 * This is untested and have shown a few issues so needs fixing first
		if (isSat) {
			for (Variable v : instance.getSlicedVariables()) {
				Object o = v.getOriginal();
				if (o instanceof SymbolicReal) {
					SymbolicReal r = (SymbolicReal) o;
					r.solution = instance.getRealValue((RealVariable) v);
					//System.out.println("r = " + r.solution);
				} else if (o instanceof SymbolicInteger) {
					SymbolicInteger r = (SymbolicInteger) o;
					r.solution = instance.getIntValue((IntVariable) v);
					//System.out.println("r = " + r.solution);
				}
			}
		}
		*/
		return isSat;
	}
	
	public boolean solveGreen() {// warning: solve calls simplify
		return solveWithSolution();
	}

	public boolean simplifyGreen() {
		if (isReplay) {
			return true;
		}
		return solveWithSolution();
	}
	
	public boolean solveOld() {// warning: solve calls simplify

		SymbolicConstraintsGeneral solver = new SymbolicConstraintsGeneral();

		boolean result1 = solver.solve(this);
		solver.cleanup();
		PathCondition.flagSolved = true;

		// modification for string path condition
		boolean result2 = spc.solve(); // TODO: to review
		return result1 && result2;
	}

	public boolean simplifyOld() {
		if(isReplay){
			return true;
		}

		SymbolicConstraintsGeneral solver = new SymbolicConstraintsGeneral();
		boolean result1;

		if (SymbolicInstructionFactory.concolicMode) {
			PCAnalyzer pa = new PCAnalyzer();
			result1 = pa.isSatisfiable(this,solver);
		}
		else
			result1 = solver.isSatisfiable(this);
		solverCalls++;
		solver.cleanup();

		if (SymbolicInstructionFactory.debugMode) {
			MinMax.Debug_no_path_constraints ++;
			if (result1)
				MinMax.Debug_no_path_constraints_sat ++;
			else
				MinMax.Debug_no_path_constraints_unsat ++;
			System.out.println("### PCs: total:" + MinMax.Debug_no_path_constraints + " sat:" +MinMax.Debug_no_path_constraints_sat + " unsat:" + MinMax.Debug_no_path_constraints_unsat +"\n");
		}

		if (! result1) return false;
		boolean result2 = spc.simplify(); // TODO to review: used for strings
		return result1  && result2;
	}

	public String stringPC() {
		return "constraint # = " + count + ((header == null) ? "" : "\n" + header.stringPC());
	}

	public String toString() {
		return "constraint # = " + count + ((header == null) ? "" : "\n" + header.toString());
		//return ((header == null) ? "" : " " + header.toString()); -- for specialization
					//+ "\n" + spc.toString(); // TODO: to review
	}
	public String prefix_notation() {
		return "constraint # = " + count + ((header == null) ? "" : "\n" + header.prefix_notation());
		//return ((header == null) ? "" : " " + header.toString()); -- for specialization
					//+ "\n" + spc.toString(); // TODO: to review
	}
	public static PathCondition getPC(MJIEnv env) {
	   VM vm = env.getVM();
	   return getPC(vm);
	}

	public static PathCondition getPC(VM vm) {
	    ChoiceGenerator<?> cg = vm.getChoiceGenerator();
	    if (cg != null && !(cg instanceof PCChoiceGenerator)) {
	        cg = cg.getPreviousChoiceGeneratorOfType(PCChoiceGenerator.class);
	    }

	    if (cg instanceof PCChoiceGenerator) {
	        return ((PCChoiceGenerator) cg).getCurrentPC();
	    } else {
	        return null;
	    }
	}

	/**
	 * Indicates whether some other object is "equal to" this one.
	 * 
	 * Note: Technically, this routine is incomplete and should take the string
	 * path condition stored in field {@code spc} into account.
	 * 
	 * @param obj
	 *            the reference object with which to compare
	 * @return {@code true} if this object is the same as the obj argument;
	 *         {@code false} otherwise.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		PathCondition p = (PathCondition) obj;
		if (count != p.count) {
			return false;
		}
		Constraint c = header;
		Constraint pc = p.header;
		while (c != null) {
			if (pc == null) {
				return false;
			}
			if (!c.equals(pc)) {
				return false;
			}
			c = c.getTail();
			pc = pc.getTail();
		}
		if (pc != null) {
			return false;
		}
		return true;
	}

	/**
	 * Compare two path conditions for orderedness. The function is based on the
	 * hash codes of the path conditions. In the event that the hash codes are
	 * equal, a lexicographic comparison is made between the constraints of the
	 * path conditions.
	 * 
	 * @param pc
	 *            the path condition to compare to
	 * @return -1 if this path condition is less than the other, +1 if it is
	 *         greater, and 0 if they are equal
	 */
	@Override
	public int compareTo(PathCondition pc) {
		int hc1 = hashCode();
		int hc2 = pc.hashCode();
		if (hc1 < hc2) {
			return -1;
		} else if (hc1 > hc2) {
			return 1;
		} else {
			// perform a lexicographic comparison
			Constraint c1 = header;
			Constraint c2 = pc.header;
			while (c1 != null) {
				if (c2 == null) {
					return 1;
				}
				int r = c1.compareTo(c2);
				if (r != 0) {
					return r;
				}
				c1 = c1.getTail();
				c2 = c2.getTail();
			}
			return (c2 == null) ? 0 : -1;
		}
	}

	/**
	 * Returns a hash code value for the object.
	 * 
	 * Note: Technically, this routine is incomplete and should take the string
	 * path condition stored in field {@code spc} into account.
	 * 
	 * @return a hash code value for this object
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		if (hashCode == null) {
			hashCode = new Integer(0);
			Constraint c = header;
			while (c != null) {
				hashCode = hashCode ^ c.hashCode();
				c = c.getTail();
			}
		}
		return hashCode;
	}

	/**
	 * Sometimes we violate our abstraction and fiddle with the fields of a path
	 * condition. Whenever the list of constraints rooted in {@link #header} is
	 * modified in any way, this routine should be called to force the
	 * re-computation of the hash value of the path condition.
	 */
	public void resetHashCode() {
		hashCode = null;
	}

	/**
	 * Recompute the value of {@link #count}, based on the actual list of
	 * constraints.
	 */
	public void recomputeCount() {
		count = 0;
		for (Constraint c = header; c != null; c = c.getTail()) {
			count++;
		}
	}

	/**
	 * Remove the header of the path condition, update the count, and reset the
	 * hash code.
	 */
	public void removeHeader() {
		spc.setRecentlyAddedConstraintUnknown(); // just to be safe, even though no one seems to be using removeHeader at all!
		assert header != null;
		header = header.and;
		count--;
		resetHashCode();
	}


}
