package com.ge.research.osate.verdict.alloy;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;

public class Util {
	/**
	 * Make a stream of an optional.
	 * 
	 * This was introduced in Java 9 but we are using Java 8 so RIP.
	 * 
	 * @param <T>
	 * @param optional
	 * @return
	 */
	public static <T> Stream<T> streamOfOptional(Optional<T> optional) {
		return optional.isPresent() ? Stream.of(optional.get()) : Stream.empty();
	}
	
	/**
	 * Find an EObject (EMF AST node) of the specified class by traversing
	 * the children of the given node.
	 * 
	 * @param <T>
	 * @param obj
	 * @param cls
	 * @return
	 */
	public static <T extends EObject> Optional<T> searchEObject(EObject obj, Class<T> cls) {
		Queue<EObject> queue = new LinkedList<>();
		queue.add(obj);
		while (!queue.isEmpty() && !(cls.isInstance(queue.peek()))) {
			EObject test = queue.poll();
			queue.addAll(test.eContents());
		}
		return queue.isEmpty() ? Optional.empty() : Optional.of(cls.cast(queue.peek()));
	}
}
