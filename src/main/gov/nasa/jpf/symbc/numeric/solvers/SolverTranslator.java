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

package gov.nasa.jpf.symbc.numeric.solvers;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.StringConstantGreen;
import za.ac.sun.cs.green.expr.StringVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.CharAtVariable;
import za.ac.sun.cs.green.expr.LengthVariable;
import za.ac.sun.cs.green.expr.IndexOfVariable;
import za.ac.sun.cs.green.expr.IndexOf2Variable;


import za.ac.sun.cs.green.expr.IndexOfCharVariable;
import za.ac.sun.cs.green.expr.IndexOfChar2Variable;

import za.ac.sun.cs.green.expr.LastIndexOfVariable;
import za.ac.sun.cs.green.expr.LastIndexOf2Variable;
import za.ac.sun.cs.green.expr.LastIndexOfCharVariable;
import za.ac.sun.cs.green.expr.LastIndexOfChar2Variable;




import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.numeric.BinaryLinearIntegerExpression;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.ConstraintExpressionVisitor;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.Operator;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;

import gov.nasa.jpf.symbc.numeric.SymbolicInteger;

import gov.nasa.jpf.symbc.string.StringConstraint;
import gov.nasa.jpf.symbc.string.StringConstant;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.symbc.string.StringExpression;

import gov.nasa.jpf.symbc.string.StringComparator;
import gov.nasa.jpf.symbc.string.StringOperator;
import gov.nasa.jpf.symbc.string.DerivedStringExpression;
import gov.nasa.jpf.symbc.string.SymbolicLengthInteger;
import gov.nasa.jpf.symbc.string.SymbolicCharAtInteger;
import gov.nasa.jpf.symbc.string.SymbolicIndexOfInteger;
import gov.nasa.jpf.symbc.string.SymbolicIndexOf2Integer;
import gov.nasa.jpf.symbc.string.SymbolicIndexOfCharInteger;
import gov.nasa.jpf.symbc.string.SymbolicIndexOfChar2Integer;
import gov.nasa.jpf.symbc.string.SymbolicLastIndexOfInteger;
import gov.nasa.jpf.symbc.string.SymbolicLastIndexOf2Integer;
import gov.nasa.jpf.symbc.string.SymbolicLastIndexOfCharInteger;
import gov.nasa.jpf.symbc.string.SymbolicLastIndexOfChar2Integer;







public class SolverTranslator {

	private static Map<ConstraintSequence, Instance> instanceCache = new HashMap<ConstraintSequence, Instance>();

	public static Instance createStringInstance(StringConstraint c) {
		Expression e = null;
		
		while (c != null) {
			Translator translator = new Translator();
			c.accept(translator);
			
			Expression tmp = translator.getExpression();
			
			if (e == null){
				e = tmp; 
			}
			else{
				e = new Operation(Operation.Operator.AND,e,tmp);
			}
			c = c.and;
		}
		Instance greenPC = new Instance(SymbolicInstructionFactory.greenSolver, null, e);
		return greenPC; 
	}
	

	public static Instance createInstance(Constraint c) {
		Expression e = null;
		
		while (c != null) {
			Translator translator = new Translator();
			c.accept(translator);
			
			Expression tmp = translator.getExpression();
			
			if (e == null){
				e = tmp; 
			}
			else{
				e = new Operation(Operation.Operator.AND,e,tmp);
			}
			c = c.and;
		}
		Instance greenPC = new Instance(SymbolicInstructionFactory.greenSolver, null, e);
		return greenPC; 
	}


	private final static class ConstraintSequence {

		private final Constraint sequence;

		public ConstraintSequence(Constraint sequence) {
			this.sequence = sequence;
		}

		@Override
		public boolean equals(Object object) {
			Constraint z = ((ConstraintSequence) object).sequence;
			Constraint s = sequence;
			while ((s != null) && (z != null)) {
				if (!s.equals(z)) {
					return false;
				}
				s = s.getTail();
				z = z.getTail();
			}
			return (s == null) && (z == null);
		}

		@Override
		public int hashCode() {
			int h = 0;
			Constraint s = sequence;
			while (s != null) {
				h ^= s.hashCode();
				s = s.getTail();
			}
			return h;
		}

	}

	private final static class Translator extends ConstraintExpressionVisitor {

		private Stack<Expression> stack;
		
		private Boolean charAt; 
		
		public Translator() {
			stack = new Stack<Expression>();
		}

		public Expression getExpression() {
			return stack.peek();
		}

		@Override
		public void postVisit(Constraint constraint) {
			Expression l;
			Expression r;
			switch (constraint.getComparator()) {
			case EQ:
				//Check if one of the operands is CharAt
				//In this case, translate the constraint as a string one 
				r = stack.pop();
				l = stack.pop();
				if (r instanceof CharAtVariable){
					//We only handle the case where the other operand is another charAt variable or an integer constant. 
					if (l instanceof CharAtVariable){
						stack.push(new Operation(Operation.Operator.EQUALS, l, r));
					}
					else if (l instanceof IntConstant){
						String val = String.valueOf((char) ((IntConstant) l).getValue());
						StringConstantGreen string_l = new StringConstantGreen(val);
						stack.push(new Operation(Operation.Operator.EQUALS, string_l, r));
					}
					else{
						System.out.println("SolverTranslator : unsupported charAt case - the other operand is not an integer constant or charAt term!");
						throw new RuntimeException();					}
				}
				else if (l instanceof CharAtVariable){
					 if (r instanceof IntConstant){
						String val = String.valueOf((char) ((IntConstant) r).getValue());
						StringConstantGreen string_r = new StringConstantGreen(val);
						stack.push(new Operation(Operation.Operator.EQUALS, l, string_r));
					}
					else{
						System.out.println("SolverTranslator : unsupported charAt case - the other operand is not an integer constant or charAt term!");
						throw new RuntimeException();	
					}
				}
				else{
					stack.push(new Operation(Operation.Operator.EQ, l, r));
				}
				break;
			case NE:
				r = stack.pop();
				l = stack.pop();
				//Check if one of the operands is CharAt
				//In this case, translate the constraint as a string one 
				if (r instanceof CharAtVariable){
					//We only handle the case where the other operand is another charAt variable or an integer constant. 
					if (l instanceof CharAtVariable){
						stack.push(new Operation(Operation.Operator.NOTEQUALS, l, r));
					}
					else if (l instanceof IntConstant){
						String val = String.valueOf((char) ((IntConstant) l).getValue());
						StringConstantGreen string_l = new StringConstantGreen(val);
						stack.push(new Operation(Operation.Operator.NOTEQUALS, string_l, r));
					}
					else{
						System.out.println("SolverTranslator : unsupported charAt case - the other operand is not an integer constant or charAt term!");
						throw new RuntimeException();					
					}
				}
				else if (l instanceof CharAtVariable){
					 if (r instanceof IntConstant){
						String val = String.valueOf((char) ((IntConstant) r).getValue());
						StringConstantGreen string_r = new StringConstantGreen(val);
						stack.push(new Operation(Operation.Operator.NOTEQUALS, l, string_r));
					}
					else{
						System.out.println("SolverTranslator : unsupported charAt case - the other operand is not an integer constant or charAt term!");
						throw new RuntimeException();	
					}
				}
				else{
					stack.push(new Operation(Operation.Operator.NE, l, r));
				}
				break;
			case LT:
				r = stack.pop();
				l = stack.pop();
				stack.push(new Operation(Operation.Operator.LT, l, r));
				break;
			case LE:
				r = stack.pop();
				l = stack.pop();
				stack.push(new Operation(Operation.Operator.LE, l, r));
				break;
			case GT:
				r = stack.pop();
				l = stack.pop();
				stack.push(new Operation(Operation.Operator.GT, l, r));
				break;
			case GE:
				r = stack.pop();
				l = stack.pop();
				stack.push(new Operation(Operation.Operator.GE, l, r));
				break;
			}

		}

		@Override
		public void postVisit(BinaryLinearIntegerExpression expression) {
			Expression l;
			Expression r;
			switch (expression.getOp()) {
			case PLUS:
				r = stack.pop();
				l = stack.pop();
				stack.push(new Operation(Operation.Operator.ADD, l, r));
				break;
			case MINUS:
				r = stack.pop();
				l = stack.pop();
				stack.push(new Operation(Operation.Operator.SUB, l, r));
				break;
			case MUL:
				r = stack.pop();
				l = stack.pop();
				stack.push(new Operation(Operation.Operator.MUL, l, r));
				break;
			default:
				System.out.println("SolverTranslator : unsupported operation " + expression.getOp());
				throw new RuntimeException();
			}
		}

		@Override
		public void postVisit(DerivedStringExpression expression) {
			Expression l;
			Expression r;
			Expression n;
			switch (expression.op) {
			case CONCAT:
				r = stack.pop();
				l = stack.pop();
				stack.push(new Operation(Operation.Operator.CONCAT, l, r));
				break;
			case REPLACE:
				r = stack.pop();
				l = stack.pop();
				n = stack.pop();
				stack.push(new Operation(Operation.Operator.REPLACE, n,l, r));
				break;
			case TRIM:
				r = stack.pop();
				stack.push(new Operation(Operation.Operator.TRIM, r));
				break;
			case REPLACEFIRST:
				r = stack.pop();
				l = stack.pop();
				n = stack.pop();
				stack.push(new Operation(Operation.Operator.REPLACEFIRST, n,l, r));
				break;
			/*case TOLOWERCASE:
				r = stack.pop();
				l = stack.pop();
				n = stack.pop();
				stack.push(new Operation(Operation.Operator.TOLOWERCASE, n,l, r));
				break;*/
			case SUBSTRING:
				r = stack.pop();
				l = stack.pop();
				int s = expression.getOperands().size();
				if(s ==3){
					n = stack.pop();
					stack.push(new Operation(Operation.Operator.SUBSTRING, n,l, r));
				}
				else{
					stack.push(new Operation(Operation.Operator.SUBSTRING, l, r));
				}
				break;
			case VALUEOF:  // This assumes that Operation.Operator.VALUEOF was incorrectly 2 (see that file).
				l = stack.pop();
				stack.push(new Operation(Operation.Operator.VALUEOF, l));
				break;				

			default:
				System.out.println("SolverTranslator : unsupported operation " + expression.op);
				throw new RuntimeException();
			}
		}

			@Override
		public void postVisit(StringConstraint constraint) {
			Expression l;
			Expression r;
			switch (constraint.getComparator()){
				case NOTEQUALS:
					r = stack.pop();
					l = stack.pop();
					stack.push(new Operation(Operation.Operator.NOTEQUALS, l, r));
					break;
				case EQUALS:
					r = stack.pop();
					l = stack.pop();
					stack.push(new Operation(Operation.Operator.EQUALS, l, r));
					break;
				case EQUALSIGNORECASE:
					r = stack.pop();
					l = stack.pop();
					stack.push(new Operation(Operation.Operator.EQUALSIGNORECASE, l, r));
					break;
				case NOTEQUALSIGNORECASE:
					r = stack.pop();
					l = stack.pop();
					stack.push(new Operation(Operation.Operator.NOTEQUALSIGNORECASE, l, r));
					break;
				case CONTAINS:
					r = stack.pop();
					l = stack.pop();
					stack.push(new Operation(Operation.Operator.CONTAINS, l, r));
					break;
				case NOTCONTAINS:
					r = stack.pop();
					l = stack.pop();
					stack.push(new Operation(Operation.Operator.NOTCONTAINS, l, r));
					break;
				case STARTSWITH:
					r = stack.pop();
					l = stack.pop();
					stack.push(new Operation(Operation.Operator.STARTSWITH, l, r));
					break;
				case NOTSTARTSWITH:
					r = stack.pop();
					l = stack.pop();
					stack.push(new Operation(Operation.Operator.NOTSTARTSWITH, l, r));
					break;
				case ENDSWITH:
					r = stack.pop();
					l = stack.pop();
					stack.push(new Operation(Operation.Operator.ENDSWITH, l, r));
					break;
				case NOTENDSWITH:
					r = stack.pop();
					l = stack.pop();
					stack.push(new Operation(Operation.Operator.NOTENDSWITH, l, r));
					break;
				default:
					System.out.println("SolverTranslator :  unsupported operation " +constraint.getComparator());

			}
			

		}

		@Override
		public void postVisit(StringConstant constant) {
			stack.push(new StringConstantGreen((String)constant.value));
		}


		@Override
		public void postVisit(IntegerConstant constant) {
			stack.push(new IntConstant((int)constant.value));
		}

		@Override
		public void postVisit(StringSymbolic variable) {
			stack.push(new StringVariable(variable.getName()));
		}



		@Override
		public void postVisit(SymbolicInteger node) {
			if (node instanceof SymbolicLengthInteger){
				SymbolicLengthInteger node_i = (SymbolicLengthInteger) node;
				StringExpression parent = node_i.getExpression();
				Translator translator_internal = new Translator();
				parent.accept(translator_internal);
				Expression parent_green = translator_internal.getExpression();
				stack.push(new LengthVariable(node.getName(), (int) node._min, (int) node._max, parent_green));
				
			}
			else if (node instanceof SymbolicCharAtInteger){
				charAt = true; 
				SymbolicCharAtInteger node_c = (SymbolicCharAtInteger) node;
				StringExpression parent = node_c.getExpression();
				IntegerExpression index = node_c.getIndex(); 
				Translator translator_internal = new Translator();
				parent.accept(translator_internal);
				Expression parent_green = translator_internal.getExpression();
				index.accept(translator_internal);
				Expression index_green = translator_internal.getExpression();
				stack.push(new CharAtVariable(node.getName(), node, (int) node._min, (int) node._max, parent_green, index_green));
			}
			else if (node instanceof SymbolicIndexOfInteger){
				SymbolicIndexOfInteger node_c = (SymbolicIndexOfInteger) node;
				StringExpression source = node_c.getSource();
				StringExpression expr = node_c.getExpression();
				Translator translator_internal = new Translator();
				source.accept(translator_internal);
				Expression source_green = translator_internal.getExpression();
				expr.accept(translator_internal);
				Expression expr_green = translator_internal.getExpression();
				stack.push(new IndexOfVariable(node.getName(), node, (int) node._min, (int) node._max, source_green, expr_green));
			}
			else if (node instanceof SymbolicIndexOf2Integer){
				SymbolicIndexOf2Integer node_c = (SymbolicIndexOf2Integer) node;
				StringExpression source = node_c.getSource();
				StringExpression expr = node_c.getExpression();
				IntegerExpression min_dist = node_c.getMinIndex();
				Translator translator_internal = new Translator();
				source.accept(translator_internal);
				Expression source_green = translator_internal.getExpression();
				expr.accept(translator_internal);
				Expression expr_green = translator_internal.getExpression();
				min_dist.accept(translator_internal);
				Expression min_dist_green = translator_internal.getExpression();
				stack.push(new IndexOf2Variable(node.getName(), node, (int) node._min, (int) node._max, source_green, expr_green,min_dist_green));
			}
			else if (node instanceof SymbolicIndexOfCharInteger){
				SymbolicIndexOfCharInteger node_c = (SymbolicIndexOfCharInteger) node;
				StringExpression source = node_c.getSource();
				IntegerExpression expr = node_c.getExpression();
				Translator translator_internal = new Translator();
				source.accept(translator_internal);
				Expression source_green = translator_internal.getExpression();
				expr.accept(translator_internal);
				Expression expr_green = translator_internal.getExpression();
				stack.push(new IndexOfCharVariable(node.getName(), node, (int) node._min, (int) node._max, source_green, expr_green));
			}
			else if (node instanceof SymbolicIndexOfChar2Integer){
				SymbolicIndexOfChar2Integer node_c = (SymbolicIndexOfChar2Integer) node;
				StringExpression source = node_c.getSource();
				IntegerExpression expr = node_c.getExpression();
				IntegerExpression min_dist = node_c.getMinDist();
				Translator translator_internal = new Translator();
				source.accept(translator_internal);
				Expression source_green = translator_internal.getExpression();
				expr.accept(translator_internal);
				Expression expr_green = translator_internal.getExpression();
				min_dist.accept(translator_internal);
				Expression min_dist_green = translator_internal.getExpression();
				stack.push(new IndexOfChar2Variable(node.getName(), node, (int) node._min, (int) node._max, source_green, expr_green,min_dist_green));
			}
			else if (node instanceof SymbolicLastIndexOfInteger){
				SymbolicLastIndexOfInteger node_c = (SymbolicLastIndexOfInteger) node;
				StringExpression source = node_c.getSource();
				StringExpression expr = node_c.getExpression();
				Translator translator_internal = new Translator();
				source.accept(translator_internal);
				Expression source_green = translator_internal.getExpression();
				expr.accept(translator_internal);
				Expression expr_green = translator_internal.getExpression();
				stack.push(new LastIndexOfVariable(node.getName(), node, (int) node._min, (int) node._max, source_green, expr_green));
			}
			else if (node instanceof SymbolicLastIndexOf2Integer){
				SymbolicLastIndexOf2Integer node_c = (SymbolicLastIndexOf2Integer) node;
				StringExpression source = node_c.getSource();
				StringExpression expr = node_c.getExpression();
				IntegerExpression min_dist = node_c.getMinIndex();
				Translator translator_internal = new Translator();
				source.accept(translator_internal);
				Expression source_green = translator_internal.getExpression();
				expr.accept(translator_internal);
				Expression expr_green = translator_internal.getExpression();
				min_dist.accept(translator_internal);
				Expression min_dist_green = translator_internal.getExpression();
				stack.push(new LastIndexOf2Variable(node.getName(), node, (int) node._min, (int) node._max, source_green, expr_green, min_dist_green));
			}
			else if (node instanceof SymbolicLastIndexOfCharInteger){
				SymbolicLastIndexOfCharInteger node_c = (SymbolicLastIndexOfCharInteger) node;
				StringExpression source = node_c.getSource();
				IntegerExpression expr = node_c.getExpression();
				Translator translator_internal = new Translator();
				source.accept(translator_internal);
				Expression source_green = translator_internal.getExpression();
				expr.accept(translator_internal);
				Expression expr_green = translator_internal.getExpression();
				stack.push(new LastIndexOfCharVariable(node.getName(), node, (int) node._min, (int) node._max, source_green, expr_green));
			}
			else if (node instanceof SymbolicLastIndexOfChar2Integer){
				SymbolicLastIndexOfChar2Integer node_c = (SymbolicLastIndexOfChar2Integer) node;
				StringExpression source = node_c.getSource();
				IntegerExpression expr = node_c.getExpression();
				IntegerExpression min_dist = node_c.getMinDist();
				Translator translator_internal = new Translator();
				source.accept(translator_internal);
				Expression source_green = translator_internal.getExpression();
				expr.accept(translator_internal);
				Expression expr_green = translator_internal.getExpression();
				min_dist.accept(translator_internal);
				Expression min_dist_green = translator_internal.getExpression();
				stack.push(new LastIndexOfChar2Variable(node.getName(), node, (int) node._min, (int) node._max, source_green, expr_green, min_dist_green));
			}
			else if (node instanceof SymbolicIndexOfChar2Integer){
				SymbolicIndexOfChar2Integer node_c = (SymbolicIndexOfChar2Integer) node;
				StringExpression source = node_c.getSource();
				IntegerExpression expr = node_c.getExpression();
				IntegerExpression min_dist = node_c.getMinDist();
				Translator translator_internal = new Translator();
				source.accept(translator_internal);
				Expression source_green = translator_internal.getExpression();
				expr.accept(translator_internal);
				Expression expr_green = translator_internal.getExpression();
				min_dist.accept(translator_internal);
				Expression min_dist_green = translator_internal.getExpression();
				stack.push(new IndexOfChar2Variable(node.getName(), node, (int) node._min, (int) node._max, source_green, expr_green, min_dist_green));
			}
			else{
				stack.push(new IntVariable(node.getName(), node, (int) node._min, (int) node._max));

			}
		}

	}

}
