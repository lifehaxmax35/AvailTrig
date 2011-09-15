/**
 * test/TypeConsistencyTest.java
 * Copyright (c) 2011, Mark van Gulik.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of the contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.avail.test;

import static org.junit.Assert.*;
import static com.avail.descriptor.TypeDescriptor.Types;
import java.io.PrintStream;
import java.util.*;
import org.junit.*;
import com.avail.descriptor.*;


/**
 * Test various consistency properties for {@linkplain TypeDescriptor types} in
 * Avail.  The type system is really pretty complex, so these tests are quite
 * important.
 *
 * <p>
 * Here are some things to test.  T is the set of types, T(x) means the type of
 * x, Co(x) is some relation that's supposed to be covariant, Con(x) is some
 * relation that's supposed to be contravariant, &cup; is type union, and &cap;
 * is type intersection.
 *
 * <table border=1 cellspacing=0>
 * <tr>
 *     <td>Subtype reflexivity</td>
 *     <td>&forall;<sub>x&isin;T</sub>&thinsp;x&sube;x</td>
 * </tr><tr>
 *     <td>Subtype transitivity</td>
 *     <td>&forall;<sub>x,y,z&isin;T</sub>&thinsp;(x&sube;y&thinsp;&and;&thinsp;y&sube;z
 *             &rarr; x&sube;z)</td>
 * </tr><tr>
 *     <td>Subtype asymmetry</td>
 *     <td>&forall;<sub>x,y&isin;T</sub>&thinsp;(x&sub;y &rarr; &not;y&sub;x)
 *         <br>
 *         <em>or alternatively,</em>
 *         <br>
 *         &forall;<sub>x,y&isin;T</sub>&thinsp;(x&sube;y&thinsp;&and;&thinsp;y&sube;x
 *         = (x=y))</td>
 * </tr><tr>
 *     <td>Union closure</td>
 *     <td>&forall;<sub>x,y&isin;T</sub>&thinsp;(x&cup;y&thinsp;&isin;&thinsp;T)</td>
 * </tr><tr>
 *     <td>Union reflexivity</td>
 *     <td>&forall;<sub>x&isin;T</sub>&thinsp;(x&cup;x&thinsp;=&thinsp;x)</td>
 * </tr><tr>
 *     <td>Union commutativity</td>
 *     <td>&forall;<sub>x,y&isin;T</sub>&thinsp;(x&cup;y = y&cup;x)</td>
 * </tr><tr>
 *     <td>Union associativity</td>
 *     <td>&forall;<sub>x,y,z&isin;T</sub>&thinsp;(x&cup;y)&cup;z = x&cup;(y&cup;z)</td>
 * </tr><tr>
 *     <td>Intersection closure</td>
 *     <td>&forall;<sub>x,y&isin;T</sub>&thinsp;(x&cap;y&thinsp;&isin;&thinsp;T)</td>
 * </tr><tr>
 *     <td>Intersection reflexivity</td>
 *     <td>&forall;<sub>x&isin;T</sub>&thinsp;(x&cap;x&thinsp;=&thinsp;x)</td>
 * </tr><tr>
 *     <td>Intersection commutativity</td>
 *     <td>&forall;<sub>x,y&isin;T</sub>&thinsp;(x&cap;y = y&cap;x)</td>
 * </tr><tr>
 *     <td>Intersection associativity</td>
 *     <td>&forall;<sub>x,y,z&isin;T</sub>&thinsp;(x&cap;y)&cap;z = x&cap;(y&cap;z)</td>
 * </tr><tr>
 *     <td>Various covariance relationships (Co)</td>
 *     <td>&forall;<sub>x,y&isin;T</sub>&thinsp;(x&sube;y &rarr; Co(x)&sube;Co(y))</td>
 * </tr><tr>
 *     <td>Various contravariance relationships (Con)</td>
 *     <td>&forall;<sub>x,y&isin;T</sub>&thinsp;(x&sube;y &rarr; Con(y)&sube;Con(x))</td>
 * </tr><tr>
 *     <td>Metacovariance</td>
 *     <td>&forall;<sub>x,y&isin;T</sub>&thinsp;(x&sube;y &rarr; T(x)&sube;T(y))</td>
 * </tr><tr>
 *     <td><em>Metavariance (preservation)*</em></td>
 *     <td>&forall;<sub>x,y&isin;T</sub>&thinsp;(x&ne;y &rarr; T(x)&ne;T(y))</td>
 * </tr>
 * </table>
 * * It's unclear if metavariance is a useful property, but it prevents the
 * destruction of information in type-manipulating expressions.
 * </p>
 * @author Mark van Gulik &lt;ghoul137@gmail.com&gt;
 */
public class TypeConsistencyTest
{
	/**
	 * {@code Node} records its instances upon creation.  They must be created
	 * in top-down order (i.e., supertypes before subtypes), as the {@link
	 * Node#Node(String, Node...) constructor} takes a variable number of
	 * supertype nodes.  The node supertype declarations are checked against the
	 * actual properties of the underlying types as one of the fundamental
	 * {@linkplain TypeConsistencyTest consistency checks}.
	 *
	 * <p>
	 * All {@link TypeDescriptor.Types} are included, as well as a few
	 * simple representative samples, such as the one-element string type and
	 * the type of whole numbers.
	 * </p>
	 */
	public abstract static class Node
	{
		/**
		 * The list of all currently defined {@linkplain Node type nodes}.
		 */
		static final List<Node> values = new ArrayList<Node>();

		/**
		 * A mapping from {@link TypeDescriptor.Types} to their corresponding
		 * {@link Node}s.
		 */
		private final static EnumMap<Types, Node> primitiveTypes =
			new EnumMap<Types, Node>(Types.class);

		static
		{
			// Include all primitive types.
			for (final Types type : Types.values())
			{
				if (!primitiveTypes.containsKey(type))
				{
					final Types typeParent = type.parent;
					final Node [] parents =
						new Node[typeParent == null ? 0 : 1];
					if (typeParent != null)
					{
						parents[0] = primitiveTypes.get(typeParent);
					}
					final Node node = new Node(type.name(), parents)
					{
						@Override AvailObject get ()
						{
							return type.o();
						}
					};
					primitiveTypes.put(type, node);
				}
			}
		}



		/** The type {@code tuple} */
		final static Node TUPLE = new Node(
			"TUPLE",
			primitiveTypes.get(Types.ANY))
		{
			@Override AvailObject get ()
			{
				return TupleTypeDescriptor.mostGeneralType();
			}
		};

		/**
		 * The type {@code string}, which is the same as {@code tuple of
		 * character}
		 */
		final static Node STRING = new Node("STRING", TUPLE)
		{
			@Override AvailObject get ()
			{
				return TupleTypeDescriptor.stringTupleType();
			}
		};

		/** The type {@code tuple [1..1] of character} */
		final static Node UNIT_STRING = new Node("UNIT_STRING", STRING)
		{
			@Override AvailObject get ()
			{
				return ByteStringDescriptor.from("x").kind();
			}
		};

		/** The type {@code type of <>} */
		final static Node EMPTY_TUPLE = new Node("EMPTY_TUPLE", TUPLE, STRING)
		{
			@Override AvailObject get ()
			{
				return TupleDescriptor.empty().kind();
			}
		};

		/** The type {@code set} */
		final static Node SET = new Node(
			"SET",
			primitiveTypes.get(Types.ANY))
		{
			@Override AvailObject get ()
			{
				return SetTypeDescriptor.mostGeneralType();
			}
		};

		/** The most general closure type. */
		final static Node MOST_GENERAL_CLOSURE = new Node(
			"MOST_GENERAL_CLOSURE",
			primitiveTypes.get(Types.ANY))
		{
			@Override AvailObject get ()
			{
				return ClosureTypeDescriptor.mostGeneralType();
			}
		};

		/**
		 * The type for closures that accept no arguments and return an integer.
		 */
		final static Node NOTHING_TO_INT_CLOSURE = new Node(
			"NOTHING_TO_INT_CLOSURE",
			MOST_GENERAL_CLOSURE)
		{
			@Override AvailObject get ()
			{
				return ClosureTypeDescriptor.create(
					TupleDescriptor.empty(),
					IntegerRangeTypeDescriptor.integers());
			}
		};

		/**
		 * The type for closures that accept an integer and return an integer.
		 */
		final static Node INT_TO_INT_CLOSURE = new Node(
			"INT_TO_INT_CLOSURE",
			MOST_GENERAL_CLOSURE)
		{
			@Override AvailObject get ()
			{
				return ClosureTypeDescriptor.create(
					TupleDescriptor.from(IntegerRangeTypeDescriptor.integers()),
					IntegerRangeTypeDescriptor.integers());
			}
		};

		/**
		 * The type for closures that accept two integers and return an integer.
		 */
		final static Node INTS_TO_INT_CLOSURE = new Node(
			"INTS_TO_INT_CLOSURE",
			MOST_GENERAL_CLOSURE)
		{
			@Override AvailObject get ()
			{
				return ClosureTypeDescriptor.create(
					TupleDescriptor.from(
						IntegerRangeTypeDescriptor.integers(),
						IntegerRangeTypeDescriptor.integers()),
					IntegerRangeTypeDescriptor.integers());
			}
		};

		/** The most specific closure type, other than terminates. */
		final static Node MOST_SPECIFIC_CLOSURE = new Node(
			"MOST_SPECIFIC_CLOSURE",
			NOTHING_TO_INT_CLOSURE,
			INT_TO_INT_CLOSURE,
			INTS_TO_INT_CLOSURE)
		{
			@Override AvailObject get ()
			{
				return ClosureTypeDescriptor.createWithArgumentTupleType(
					TupleTypeDescriptor.mostGeneralType(),
					TerminatesTypeDescriptor.terminates(),
					SetDescriptor.empty());
			}
		};

		/** The primitive type representing the extended integers [-∞..∞]. */
		final static Node EXTENDED_INTEGER = new Node(
			"EXTENDED_INTEGER",
			primitiveTypes.get(Types.ANY))
		{
			@Override AvailObject get ()
			{
				return IntegerRangeTypeDescriptor.extendedIntegers();
			}
		};

		/** The primitive type representing whole numbers [0..∞). */
		final static Node WHOLE_NUMBER = new Node(
			"WHOLE_NUMBER",
			EXTENDED_INTEGER)
		{
			@Override AvailObject get ()
			{
				return IntegerRangeTypeDescriptor.wholeNumbers();
			}
		};

		/** Some {@linkplain AtomDescriptor atom}'s instance type. */
		final static Node SOME_ATOM_TYPE = new Node(
			"SOME_ATOM_TYPE",
			primitiveTypes.get(Types.ATOM))
		{
			@Override AvailObject get ()
			{
				return InstanceTypeDescriptor.withInstance(
					AtomDescriptor.create(
						ByteStringDescriptor.from("something")));
			}
		};

		/**
		 * The instance type of an {@linkplain AtomDescriptor atom} different
		 * from {@link #SOME_ATOM_TYPE}.
		 */
		final static Node ANOTHER_ATOM_TYPE = new Node(
			"ANOTHER_ATOM_TYPE",
			primitiveTypes.get(Types.ATOM))
		{
			@Override AvailObject get ()
			{
				return InstanceTypeDescriptor.withInstance(
					AtomDescriptor.create(
						ByteStringDescriptor.from("another")));
			}
		};

		/**
		 * The base {@linkplain ObjectTypeDescriptor object type}.
		 */
		final static Node OBJECT_TYPE = new Node(
			"OBJECT_TYPE",
			primitiveTypes.get(Types.ANY))
		{
			@Override AvailObject get ()
			{
				return ObjectTypeDescriptor.mostGeneralType();
			}
		};

		/**
		 * A simple non-root {@linkplain ObjectTypeDescriptor object type}.
		 */
		final static Node NON_ROOT_OBJECT_TYPE = new Node(
			"NON_ROOT_OBJECT_TYPE",
			OBJECT_TYPE)
		{
			@Override AvailObject get ()
			{
				return ObjectTypeDescriptor.objectTypeFromMap(
					MapDescriptor.empty().mapAtPuttingCanDestroy(
						SOME_ATOM_TYPE.t,
						TypeDescriptor.Types.ANY.o(),
						false));
			}
		};

		/**
		 * A simple non-root {@linkplain ObjectTypeDescriptor object type}.
		 */
		final static Node NON_ROOT_OBJECT_TYPE_WITH_INTEGERS = new Node(
			"NON_ROOT_OBJECT_TYPE_WITH_INTEGERS",
			NON_ROOT_OBJECT_TYPE)
		{
			@Override AvailObject get ()
			{
				return ObjectTypeDescriptor.objectTypeFromMap(
					MapDescriptor.empty().mapAtPuttingCanDestroy(
						SOME_ATOM_TYPE.t,
						IntegerRangeTypeDescriptor.integers(),
						false));
			}
		};

		/**
		 * The metatype for closure types.
		 */
		final static Node CLOSURE_META = new Node(
			"CLOSURE_META",
			primitiveTypes.get(Types.TYPE))
		{
			@Override AvailObject get ()
			{
				return ClosureTypeDescriptor.meta();
			}
		};

		/**
		 * The metatype for continuation types.
		 */
		final static Node CONTINUATION_META = new Node(
			"CONTINUATION_META",
			primitiveTypes.get(Types.TYPE))
		{
			@Override AvailObject get ()
			{
				return ContinuationTypeDescriptor.meta();
			}
		};

		/**
		 * The metatype for integer types.
		 */
		final static Node INTEGER_META = new Node(
			"INTEGER_META",
			primitiveTypes.get(Types.TYPE))
		{
			@Override AvailObject get ()
			{
				return IntegerRangeTypeDescriptor.meta();
			}
		};

		/** The primitive type representing the metatype of whole numbers [0..∞). */
		final static Node WHOLE_NUMBER_META = new Node(
			"WHOLE_NUMBER_META",
			INTEGER_META,
			primitiveTypes.get(Types.TYPE))
		{
			@Override AvailObject get ()
			{
				return InstanceTypeDescriptor.withInstance(
					IntegerRangeTypeDescriptor.wholeNumbers());
			}
		};

		/** The primitive type representing the metametatype of the metatype of whole numbers [0..∞). */
		final static Node WHOLE_NUMBER_META_META = new Node(
			"WHOLE_NUMBER_META_META",
			primitiveTypes.get(Types.META),
			primitiveTypes.get(Types.UNION_TYPE))
		{
			@Override AvailObject get ()
			{
				return InstanceTypeDescriptor.withInstance(
					InstanceTypeDescriptor.withInstance(
						IntegerRangeTypeDescriptor.wholeNumbers()));
			}
		};

		/**
		 * The metatype for map types.
		 */
		final static Node MAP_META = new Node(
			"MAP_META",
			primitiveTypes.get(Types.TYPE))
		{
			@Override AvailObject get ()
			{
				return MapTypeDescriptor.meta();
			}
		};

		/**
		 * The metatype for set types.
		 */
		final static Node SET_META = new Node(
			"SET_META",
			primitiveTypes.get(Types.TYPE))
		{
			@Override AvailObject get ()
			{
				return SetTypeDescriptor.meta();
			}
		};

		/**
		 * The metatype for tuple types.
		 */
		final static Node TUPLE_META = new Node(
			"TUPLE_META",
			primitiveTypes.get(Types.TYPE))
		{
			@Override AvailObject get ()
			{
				return TupleTypeDescriptor.meta();
			}
		};


		/** The type of {@code terminates}.  This is the most specific meta. */
		final static Node TERMINATES_TYPE = new Node(
			"TERMINATES_TYPE",
			CLOSURE_META,
			primitiveTypes.get(Types.CONTAINER_TYPE),
			CONTINUATION_META,
			WHOLE_NUMBER_META,
			WHOLE_NUMBER_META_META,
			MAP_META,
			SET_META,
			TUPLE_META,
			primitiveTypes.get(Types.META),
			primitiveTypes.get(Types.UNION_TYPE))
		{
			@Override AvailObject get ()
			{
				return InstanceTypeDescriptor.withInstance(
					TerminatesTypeDescriptor.terminates());
			}
		};

		/**
		 * The list of all {@link Node}s except TERMINATES.
		 */
		private final static List<Node> nonTerminatesTypes =
			new ArrayList<Node>();

		static
		{
			for (final Node existingType : values)
			{
				nonTerminatesTypes.add(existingType);
			}
		}

		/** The type {@code terminates} */
		final static Node TERMINATES = new Node(
			"TERMINATES",
			nonTerminatesTypes.toArray(new Node[0]))
		{
			@Override AvailObject get ()
			{
				return TerminatesTypeDescriptor.terminates();
			}
		};




		/** The name of this type node, used for error diagnostics. */
		final String name;

		/** The Avail {@link TypeDescriptor type} I represent in the graph. */
		AvailObject t;

		/** A unique 0-based index for this {@code Node}. */
		final int index;

		/** The supernodes in the graph. */
		final Node [] supernodes;


		/** The subnodes in the graph, as an {@link EnumSet}. */
		private Set<Node> subnodes;


		/** Every node descended from this on, as an {@link EnumSet}. */
		Set<Node> allDescendants;

		/**
		 * A cache of type unions where I'm the left participant and the right
		 * participant (a Node) supplies its index for accessing the array.
		 */
		private AvailObject unionCache[];

		/**
		 * A cache of type intersections where I'm the left participant and the
		 * right participant (a Node) supplies its index for accessing the
		 * array.
		 */
		private AvailObject intersectionCache[];

		/**
		 * A cache of subtype tests where I'm the proposed subtype and the
		 * argument is the proposed supertype.  The value stored indicates if
		 * I am a subtype of the argument.
		 */
		private Boolean subtypeCache[];

		/**
		 * Construct a new {@link Node}, capturing a varargs list of known
		 * supertypes.
		 *
		 * @param name
		 *            The printable name of this {@link Node}.
		 * @param supernodes
		 *            The array of {@linkplain Node nodes} that this node is
		 *            asserted to descend from.  Transitive ancestors may be
		 *            elided.
		 */
		Node (final String name, final Node... supernodes)
		{
			this.name = name;
			this.supernodes = supernodes;
			this.index = values.size();
			values.add(this);
		}


		/* The nodes' slots have to be initialized here because they pass
		 * the Node.class to the EnumSet factory, which attempts to
		 * determine the number of enumeration values, which isn't known yet
		 * when the constructors are still running.
		 *
		 * Also build the inverse and (downwards) transitive closure at each
		 * node of the graph, since they're independent of how the actual types
		 * are related.  Discrepancies between the graph information and the
		 * actual types is resolved in {@link
		 * TypeConsistencyTest#testGraphModel()}.
		 */
		static
		{
			for (final Node node : values)
			{
				node.subnodes = new HashSet<Node>();
				node.allDescendants = new HashSet<Node>();
			}
			for (final Node node : values)
			{
				for (final Node supernode : node.supernodes)
				{
					supernode.subnodes.add(node);
				}
			}
			for (final Node node : values)
			{
				node.allDescendants.add(node);
				node.allDescendants.addAll(node.subnodes);
			}
			boolean changed;
			do
			{
				changed = false;
				for (final Node node : values)
				{
					for (final Node subnode : node.subnodes)
					{
						changed |= node.allDescendants.addAll(
							subnode.allDescendants);
					}
				}
			}
			while (changed);
		}


		/**
		 * Enumeration instances are required to implement this to construct the
		 * actual Avail {@linkplain TypeDescriptor type} that this {@link Node}
		 * represents.
		 *
		 * @return The {@link AvailObject} that is the {@linkplain
		 *         TypeDescriptor type} that this {@link Node} represents.
		 */
		abstract AvailObject get ();


		/**
		 * Lookup or compute and cache the type union of the receiver's {@link
		 * #t} and the argument's {@code t}.
		 *
		 * @param rightNode
		 *            The {@linkplain Node} for the right side of the union.
		 * @return
		 *            The {@linkplain AvailObject#typeUnion(AvailObject) type
		 *            union} of the receiver's {@link #t} and the argument's
		 *            {@code t}.
		 */
		AvailObject union (final Node rightNode)
		{
			final int rightIndex = rightNode.index;
			AvailObject union = unionCache[rightIndex];
			if (union == null)
			{
				union = t.typeUnion(rightNode.t).makeImmutable();
				unionCache[rightIndex] = union;
			}
			return union;
		}

		/**
		 * Lookup or compute and cache the type intersection of the receiver's
		 * {@link #t} and the argument's {@code t}.
		 *
		 * @param rightNode
		 *            The {@linkplain Node} for the right side of the
		 *            intersection.
		 * @return
		 *            The {@linkplain AvailObject#typeIntersection(AvailObject)
		 *            type intersection} of the receiver's {@link #t} and the
		 *            argument's {@code t}.
		 */
		AvailObject intersect (final Node rightNode)
		{
			final int rightIndex = rightNode.index;
			AvailObject intersection = intersectionCache[rightIndex];
			if (intersection == null)
			{
				intersection = t.typeIntersection(rightNode.t).makeImmutable();
				intersectionCache[rightIndex] = intersection;
			}
			return intersection;
		}

		/**
		 * Lookup or compute and cache whether the receiver's {@link #t} is a
		 * subtype of the argument's {@code t}.
		 *
		 * @param rightNode
		 *            The {@linkplain Node} for the right side of the subtype
		 *            test.
		 * @return
		 *            Whether the receiver's {@link #t} is a subtype of the
		 *            argument's {@code t}.
		 */
		boolean subtype (final Node rightNode)
		{
			final int rightIndex = rightNode.index;
			Boolean subtype = subtypeCache[rightIndex];
			if (subtype == null)
			{
				subtype = t.isSubtypeOf(rightNode.t);
				subtypeCache[rightIndex] = subtype;
			}
			return subtype;
		}

		@Override
		public String toString()
		{
			return name;
		};

		/**
		 * Record the actual type information into the graph.
		 */
		static void createTypes ()
		{
			final int n = values.size();
			for (final Node node : values)
			{
				node.t = node.get();
				node.unionCache = new AvailObject[n];
				node.intersectionCache = new AvailObject[n];
				node.subtypeCache = new Boolean[n];
			}
		}

		/**
		 * Remove all type information from the graph, leaving the shape intact.
		 */
		static void eraseTypes ()
		{
			for (final Node node : values)
			{
				node.t = null;
				node.unionCache = null;
				node.intersectionCache = null;
				node.subtypeCache = null;
			}
		}

	}


	/**
	 * Test fixture: clear and then create all special objects well-known to the
	 * Avail runtime, then set up the graph of types.
	 */
	@BeforeClass
	public static void initializeAllWellKnownObjects ()
	{
		AvailObject.clearAllWellKnownObjects();
		AvailObject.createAllWellKnownObjects();
		Node.createTypes();
		System.out.format("Checking %d types%n", Node.values.size());

		// dumpGraphTo(System.out);
	}



	/**
	 * Output a machine-readable representation of the graph as a sequence of
	 * lines of text.  First output the number of nodes, then the single-quoted
	 * node names in some order.  Then output all edges as parethesis-enclosed
	 * space-separated pairs of zero-based indices into the list of nodes.  The
	 * first element is the subtype, the second is the supertype.  The graph has
	 * not been reduced to eliminate redundant edges.
	 *
	 * <p>
	 * The nodes include everything in {Node.values}, as well as all type unions
	 * and type intersections of two or three of these base elements, including
	 * the left and right associative versions in case the type system is
	 * incorrect.
	 * </p>
	 *
	 * @param out
	 *            A PrintStream on which to dump a representation of the current
	 *            type graph.
	 */
	public static void dumpGraphTo (final PrintStream out)
	{
		final Set<AvailObject> allTypes = new HashSet<AvailObject>();
		for (final Node node : Node.values)
		{
			allTypes.add(node.t);
		}
		for (final Node t1 : Node.values)
		{
			for (final Node t2 : Node.values)
			{
				final AvailObject union12 = t1.union(t2);
				allTypes.add(union12);
				final AvailObject inter12 = t1.intersect(t2);
				allTypes.add(inter12);
				for (final Node t3 : Node.values)
				{
					allTypes.add(union12.typeUnion(t3.t));
					allTypes.add(t3.t.typeUnion(union12));
					allTypes.add(inter12.typeIntersection(t3.t));
					allTypes.add(t3.t.typeIntersection(inter12));
				}
			}
		}
		final List<AvailObject> allTypesList = new ArrayList<AvailObject>(allTypes);
		final Map<AvailObject,Integer> inverse = new HashMap<AvailObject,Integer>();
		final String[] names = new String[allTypes.size()];
		for (int i = 0; i < allTypesList.size(); i++)
		{
			inverse.put(allTypesList.get(i), i);
		}
		for (final Node node : Node.values)
		{
			names[inverse.get(node.t)] = "#" + node.name;
		}
		for (int i = 0; i < allTypesList.size(); i++)
		{
			if (names[i] == null)
			{
				names[i] = allTypesList.get(i).toString();
			}
		}

		out.println(allTypesList.size());
		for (int i1 = 0; i1 < allTypesList.size(); i1++)
		{
			out.println("\'" + names[i1] + "\'");
		}
		for (int i1 = 0; i1 < allTypes.size(); i1++)
		{
			for (int i2 = 0; i2 < allTypes.size(); i2++)
			{
				if (allTypesList.get(i1).isSubtypeOf(allTypesList.get(i2)))
				{
					out.println("(" + i1 + " " + i2 + ")");
				}

			}
		}
	}



	/**
	 * Test fixture: clear all special objects, wiping each {@link Node}'s type.
	 */
	@AfterClass
	public static void clearAllWellKnownObjects ()
	{
		AvailObject.clearAllWellKnownObjects();
		Node.eraseTypes();
	}


	/**
	 * Compare the first two arguments for {@linkplain Object#equals(Object)
	 * equality}.  If unequal, use the supplied message pattern and message
	 * arguments to construct an error message, then fail with it.
	 *
	 * @param a The first object to compare.
	 * @param b The second object to compare.
	 * @param messagePattern
	 *            A format string for producing an error message in the event
	 *            that the objects are not equal.
	 * @param messageArguments
	 *            A variable number of objects to describe via the
	 *            messagePattern.
	 */
	void assertEQ (
		final Object a,
		final Object b,
		final String messagePattern,
		final Object... messageArguments)
	{
		if (!a.equals(b))
		{
			fail(String.format(messagePattern, messageArguments));
		}
	}

	/**
	 * Examine the first (boolean) argument.  If false, use the supplied message
	 * pattern and message arguments to construct an error message, then fail
	 * with it.
	 *
	 * @param bool
	 *            The boolean which should be true for success.
	 * @param messagePattern
	 *            A format string for producing an error message in the event
	 *            that the supplied boolean was false.
	 * @param messageArguments
	 *            A variable number of objects to describe via the
	 *            messagePattern.
	 */
	void assertT (
		final boolean bool,
		final String messagePattern,
		final Object... messageArguments)
	{
		if (!bool)
		{
			fail(String.format(messagePattern, messageArguments));
		}
	}

	/**
	 * Test that the {@linkplain Node#supernodes declared} subtype relations
	 * actually hold the way the graph says they should.
	 */
	@Test
	public void testGraphModel ()
	{
		for (final Node x : Node.values)
		{
			for (final Node y : Node.values)
			{
				assertEQ(
					y.allDescendants.contains(x),
					x.subtype(y),
					"graph model (not as declared): %s, %s",
					x,
					y);
				assertEQ(
					x == y,
					x.t.equals(y.t),
					"graph model (not unique) %s, %s",
					x,
					y);
			}
		}
	}

	/**
	 * Test that the subtype relationship is reflexive.
	 * <span style="border-width:thin; border-style:solid"><nobr>
	 * &forall;<sub>x&isin;T</sub>&thinsp;x&sube;x
	 * </nobr></span>
	 */
	@Test
	public void testSubtypeReflexivity ()
	{
		for (final Node x : Node.values)
		{
			assertT(
				x.subtype(x),
				"subtype reflexivity: %s",
				x);
		}
	}

	/**
	 * Test that the subtype relationship is transitive.
	 * <span style="border-width:thin; border-style:solid"><nobr>
	 * &forall;<sub>x,y,z&isin;T</sub>&thinsp;(x&sube;y&thinsp;&and;&thinsp;y&sube;z
	 *     &rarr; x&sube;z)
	 * </nobr></span>
	 */
	@Test
	public void testSubtypeTransitivity ()
	{
		for (final Node x : Node.values)
		{
			for (final Node y : Node.values)
			{
				for (final Node z : Node.values)
				{
					assertT(
						(!(x.subtype(y) && y.subtype(z)))
							|| x.subtype(z),
						"subtype transitivity: %s, %s, %s",
						x,
						y,
						z);
				}
			}
		}
	}

	/**
	 * Test that the subtype relationship is asymmetric.
	 * <span style="border-width:thin; border-style:solid"><nobr>
	 * &forall;<sub>x,y&isin;T</sub>&thinsp;(x&sub;y &rarr; &not;y&sub;x)
	 * </nobr></span>
	 */
	@Test
	public void testSubtypeAsymmetry ()
	{
		for (final Node x : Node.values)
		{
			for (final Node y : Node.values)
			{
				assertEQ(
					x.subtype(y) && y.subtype(x),
					x == y,
					"subtype asymmetry: %s, %s",
					x,
					y);
			}
		}
	}

	/**
	 * Test that types are closed with respect to the type union operator.
	 * <span style="border-width:thin; border-style:solid"><nobr>
	 * &forall;<sub>x,y&isin;T</sub>&thinsp;(x&cup;y&thinsp;&isin;&thinsp;T)
	 * </nobr></span>
	 */
	@Test
	public void testUnionClosure ()
	{
		for (final Node x : Node.values)
		{
			for (final Node y : Node.values)
			{
				assertT(
					x.union(y).isInstanceOf(Types.TYPE.o()),
					"union closure: %s, %s",
					x,
					y);
			}
		}
	}

	/**
	 * Test that the type union operator is reflexive.
	 * <span style="border-width:thin; border-style:solid"><nobr>
	 * &forall;<sub>x&isin;T</sub>&thinsp;(x&cup;x&thinsp;=&thinsp;x)
	 * </nobr></span>
	 */
	@Test
	public void testUnionReflexivity ()
	{
		for (final Node x : Node.values)
		{
			assertEQ(
				x.union(x),
				x.t,
				"union reflexivity: %s",
				x);
		}
	}

	/**
	 * Test that the type union operator is commutative.
	 * <span style="border-width:thin; border-style:solid"><nobr>
	 * &forall;<sub>x,y&isin;T</sub>&thinsp;(x&cup;y = y&cup;x)
	 * </nobr></span>
	 */
	@Test
	public void testUnionCommutativity ()
	{
		for (final Node x : Node.values)
		{
			for (final Node y : Node.values)
			{
				assertEQ(
					x.union(y),
					y.union(x),
					"union commutativity: %s, %s",
					x,
					y);
			}
		}
	}

	/**
	 * Test that the type union operator is associative.
	 * <span style="border-width:thin; border-style:solid"><nobr>
	 * &forall;<sub>x,y,z&isin;T</sub>&thinsp;(x&cup;y)&cup;z = x&cup;(y&cup;z)
	 * </nobr></span>
	 */
	@Test
	public void testUnionAssociativity ()
	{
		for (final Node x : Node.values)
		{
			for (final Node y : Node.values)
			{
				final AvailObject xy = x.union(y);
				for (final Node z : Node.values)
				{
					assertEQ(
						xy.typeUnion(z.t),
						x.t.typeUnion(y.union(z)),
						"union associativity: %s, %s, %s",
						x,
						y,
						z);
				}
			}
		}
	}

	/**
	 * Test that types are closed with respect to the type intersection
	 * operator.
	 * <span style="border-width:thin; border-style:solid"><nobr>
	 * &forall;<sub>x,y&isin;T</sub>&thinsp;(x&cap;y&thinsp;&isin;&thinsp;T)
	 * </nobr></span>
	 */
	@Test
	public void testIntersectionClosure ()
	{
		for (final Node x : Node.values)
		{
			for (final Node y : Node.values)
			{
				assertT(
					x.intersect(y).isInstanceOf(Types.TYPE.o()),
					"intersection closure: %s, %s",
					x,
					y);
			}
		}
	}

	/**
	 * Test that the type intersection operator is reflexive.
	 * <span style="border-width:thin; border-style:solid"><nobr>
	 * &forall;<sub>x&isin;T</sub>&thinsp;(x&cap;x&thinsp;=&thinsp;x)
	 * </nobr></span>
	 */
	@Test
	public void testIntersectionReflexivity ()
	{
		for (final Node x : Node.values)
		{
			assertEQ(
				x.intersect(x),
				x.t,
				"intersection reflexivity: %s",
				x);
		}
	}

	/**
	 * Test that the type intersection operator is commutative.
	 * <span style="border-width:thin; border-style:solid"><nobr>
	 * &forall;<sub>x,y&isin;T</sub>&thinsp;(x&cap;y = y&cap;x)
	 * </nobr></span>
	 */
	@Test
	public void testIntersectionCommutativity ()
	{
		for (final Node x : Node.values)
		{
			for (final Node y : Node.values)
			{
				assertEQ(
					x.intersect(y),
					y.intersect(x),
					"intersection commutativity: %s, %s",
					x,
					y);
			}
		}
	}

	/**
	 * Test that the type intersection operator is associative.
	 * <span style="border-width:thin; border-style:solid"><nobr>
	 * &forall;<sub>x,y,z&isin;T</sub>&thinsp;(x&cap;y)&cap;z = x&cap;(y&cap;z)
	 * </nobr></span>
	 */
	@Test
	public void testIntersectionAssociativity ()
	{
		for (final Node x : Node.values)
		{
			for (final Node y : Node.values)
			{
				final AvailObject xy = x.intersect(y);
				for (final Node z : Node.values)
				{
					assertEQ(
						xy.typeIntersection(z.t),
						x.t.typeIntersection(y.intersect(z)),
						"intersection associativity: %s, %s, %s",
						x,
						y,
						z);
				}
			}
		}
	}

	/**
	 * Test that the subtype relation covaries with closure return type.
	 * <span style="border-width:thin; border-style:solid"><nobr>
	 * &forall;<sub>x,y&isin;T</sub>&thinsp;(x&sube;y &rarr; Co(x)&sube;Co(y))
	 * </nobr></span>
	 */
	@Test
	public void testClosureResultCovariance ()
	{
		for (final Node x : Node.values)
		{
			for (final Node y : Node.values)
			{
				final AvailObject CoX = ClosureTypeDescriptor.create(
					TupleDescriptor.empty(),
					x.t);
				final AvailObject CoY = ClosureTypeDescriptor.create(
					TupleDescriptor.empty(),
					y.t);
				assertT(
					!x.subtype(y) || CoX.isSubtypeOf(CoY),
					"covariance (closure result): %s, %s",
					x,
					y);
			}
		}
	}

	/**
	 * Test that the subtype relation covaries with (homogeneous) tuple element
	 * type.
	 * <span style="border-width:thin; border-style:solid"><nobr>
	 * &forall;<sub>x,y&isin;T</sub>&thinsp;(x&sube;y &rarr; Co(x)&sube;Co(y))
	 * </nobr></span>
	 */
	@Test
	public void testTupleEntryCovariance ()
	{
		for (final Node x : Node.values)
		{
			for (final Node y : Node.values)
			{
				final AvailObject CoX =
					TupleTypeDescriptor.tupleTypeForSizesTypesDefaultType(
						IntegerRangeTypeDescriptor.wholeNumbers(),
						TupleDescriptor.empty(),
						x.t);
				final AvailObject CoY =
					TupleTypeDescriptor.tupleTypeForSizesTypesDefaultType(
						IntegerRangeTypeDescriptor.wholeNumbers(),
						TupleDescriptor.empty(),
						y.t);
				assertT(
					!x.subtype(y) || CoX.isSubtypeOf(CoY),
					"covariance (tuple entries): %s, %s",
					x,
					y);
			}
		}
	}

	/**
	 * Test that the subtype relation <em>contravaries</em> with closure
	 * argument type.
	 * <span style="border-width:thin; border-style:solid"><nobr>
	 * &forall;<sub>x,y&isin;T</sub>&thinsp;(x&sube;y &rarr; Con(y)&sube;Con(x))
	 * </nobr></span>
	 */
	@Test
	public void testClosureArgumentContravariance ()
	{
		for (final Node x : Node.values)
		{
			for (final Node y : Node.values)
			{
				final AvailObject ConX = ClosureTypeDescriptor.create(
					TupleDescriptor.from(
						x.t),
					Types.TOP.o());
				final AvailObject ConY = ClosureTypeDescriptor.create(
					TupleDescriptor.from(
						y.t),
					Types.TOP.o());
				assertT(
					!x.subtype(y) || ConY.isSubtypeOf(ConX),
					"contravariance (closure argument): %s, %s",
					x,
					y);
			}
		}
	}

	/**
	 * Check that the subtype relation covaries under the "type-of" mapping.
	 * This is simply covariance of metatypes, which is abbreviated as
	 * metacovariance.
	 * <span style="border-width:thin; border-style:solid"><nobr>
	 * &forall;<sub>x,y&isin;T</sub>&thinsp;(x&sube;y &rarr; T(x)&sube;T(y))
	 * </nobr></span>
	 */
	@Test
	public void testMetacovariance ()
	{
		for (final Node x : Node.values)
		{
			for (final Node y : Node.values)
			{
				final AvailObject Tx = InstanceTypeDescriptor.withInstance(x.t);
				final AvailObject Ty = InstanceTypeDescriptor.withInstance(y.t);
				assertT(
					!x.subtype(y) || Tx.isSubtypeOf(Ty),
					"metacovariance: %s, %s",
					x,
					y);
			}
		}
	}

	/**
	 * Check that a type and a proper subtype are distinct after transformation
	 * through the "type-of" mapping.
	 * <span style="border-width:thin; border-style:solid"><nobr>
	 * &forall;<sub>x,y&isin;T</sub>&thinsp;(x&ne;y &equiv; T(x)&ne;T(y))
	 * </nobr></span>
	 */
	//@Test
	public void testMetavariance ()
	{
		for (final Node x : Node.values)
		{
			for (final Node y : Node.values)
			{
				final AvailObject Tx = x.t.kind();
				final AvailObject Ty = y.t.kind();
				assertEQ(
					x.t.equals(y.t),
					Tx.equals(Ty),
					"metavariance: %s, %s",
					x,
					y);
			}
		}
	}
}