package com.tado.mspsim.utils;

/******************************************************************************
 * File: RingBuffer.java 
 * 
 * Author: Keith Schwarz (htiek@cs.stanford.edu)
 * 
 * An implementation of a synchronized queue backed by a ring buffer. This
 * functionality and implementation is similar to the ArrayBlockingQueue class,
 * but I thought that I'd implement my own version to get a better feel for how
 * it works.
 * 
 * A ring buffer is a space-efficient, locality-friendly implementation of a
 * FIFO queue. It is implemented as a fixed-sized array that is treated as
 * though it wraps around like a ring; it has no well-defined start or end
 * point. This array stores two pointers, a read pointer and a write pointer,
 * delineating where the next insert should take place and from where the next
 * element should be dequeued. For example:
 * 
 * [2] [3] [ ] [ ] [ ] [ ] [0] [1] 
 *      ^                   ^ 
 *      |                   | 
 *    write                read
 * 
 * When using a ring buffer, one must be careful not to let the read and write
 * pointers cross one another. If this happens, future write operations will
 * start overwriting old elements that have not yet been consumed. For this
 * reason, most ring buffers adopt one of two strategies. First, the ring buffer
 * can increase its size whenever it runs out of room. This approach allows the
 * buffer to grow arbitrarily large if need be. The second option, and the one
 * used in this implementation, is simply to block on a read or write when data
 * is not available. This allows the ring buffer to implement the
 * producer/consumer pattern fairly easily; any number of threads can begin
 * creating data while some number of threads consume it, and at no time are too
 * many elements kept in memory waiting to be read.
 */

public final class RingBuffer<T> {
	/* The actual ring buffer. */
	private final T[] elements;

	/* The write pointer, represented as an offset into the array. */
	private int offset = 0;

	/*
	 * The read pointer is encoded implicitly by keeping track of the number of
	 * unconsumed elements. We can then determine its position by backing up that
	 * many positions before the read position.
	 */
	private int unconsumedElements = 0;

	/**
	 * Constructs a new RingBuffer with the specified capacity, which must be
	 * positive.
	 * 
	 * @param size
	 *           The capacity of the new ring buffer.
	 * @throws IllegalArgumentException
	 *            If the capacity is negative.
	 */
	@SuppressWarnings("unchecked")
	public RingBuffer(int size) {
		/* Validate the size. */
		if (size <= 0)
			throw new IllegalArgumentException(
					"RingBuffer capacity must be positive.");

		/* Construct the array to be that size. */
		elements = (T[]) new Object[size];
	}

	/**
	 * Clear the buffer
	 */
	@SuppressWarnings("unchecked")
	public synchronized void clear() {
		offset = 0;
		unconsumedElements = 0;
		for (int i = 0; i < elements.length; i++) {
			elements[i] = null;
		}
	}

	/**
	 * Appends an element to the ring buffer, blocking until space becomes
	 * available.
	 * 
	 * @param elem
	 *           The element to add to the ring buffer.
	 */
	public synchronized boolean add(T elem) {
		/*
		 * Block until the capacity is nonzero. Otherwise we don't have any space
		 * to write.
		 */
		if (unconsumedElements == elements.length)
			return false;

		/*
		 * Write the element into the next open spot, then advance the write
		 * pointer forward a step.
		 */
		elements[offset] = elem;
		offset = (offset + 1) % elements.length;

		/*
		 * Increase the number of unconsumed elements by one, then notify any
		 * threads that are waiting that more data is now available.
		 */
		++unconsumedElements;
		return true;
	}

	/**
	 * Returns the maximum capacity of the ring buffer.
	 * 
	 * @return The maximum capacity of the ring buffer.
	 */
	public int capacity() {
		return elements.length;
	}

	/**
	 * Observes, but does not dequeue, the next available element, blocking until
	 * data becomes available.
	 * 
	 * @return The next available element.
	 */
	public synchronized T peek() {
		/* Wait for data to become available. */
		if (unconsumedElements == 0)
			return null;

		/*
		 * Hand back the next value. The index of this next value is a bit tricky
		 * to compute. We know that there are unconsumedElements elements waiting
		 * to be read, and they're contiguously before the write position.
		 * However, the buffer wraps around itself, and so we can't just do a
		 * naive subtraction; that might end up giving us a negative index. To
		 * avoid this, we'll use a clever trick in which we'll add to the index
		 * the capacity minus the distance. This value must be positive, since the
		 * distance is never greater than the capacity, and if we then wrap this
		 * value around using the modulus operator we'll end up with a valid
		 * index. All of this machinery works because
		 * 
		 * (x + (n - k)) mod n == (x - k) mod n
		 * 
		 * And Java's modulus operator works best on positive values.
		 */
		return elements[(offset + (capacity() - unconsumedElements)) % capacity()];
	}

	/**
	 * Removes and returns the next available element, blocking until data
	 * becomes available.
	 * 
	 * @return The next available element
	 */
	public synchronized T remove() {
		/* Use peek() to get the element to return. */
		T result = peek();

		/* Mark that one fewer elements are now available to read. */
		--unconsumedElements;

		/* Because there is more space left, wake up any waiting threads. */
		notifyAll();

		return result;
	}

	/**
	 * Returns the number of elements that are currently being stored in the ring
	 * buffer.
	 * 
	 * @return The number of elements currently stored in the ring buffer.
	 */
	public synchronized int size() {
		return unconsumedElements;
	}

	/**
	 * Returns whether the ring buffer is empty.
	 * 
	 * @return Whether the ring buffer is empty.
	 */
	public synchronized boolean isEmpty() {
		return size() == 0;
	}
}